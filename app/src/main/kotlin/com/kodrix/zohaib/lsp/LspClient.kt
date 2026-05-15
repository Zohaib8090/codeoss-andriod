package com.kodrix.zohaib.lsp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

class LspClient(
    private val command: List<String>,
    private val workingDir: java.io.File,
    private val env: Map<String, String>
) {
    private var process: Process? = null
    private var stdin: OutputStream? = null
    private var stdout: BufferedInputStream? = null
    
    private val gson = Gson()
    private val messageId = AtomicInteger(1)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    var onDiagnosticsReceived: ((PublishDiagnosticsParams) -> Unit)? = null
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()

    fun start() {
        val pb = ProcessBuilder(command)
        pb.directory(workingDir)
        pb.environment().putAll(env)
        
        try {
            process = pb.start()
            stdin = process?.outputStream
            stdout = BufferedInputStream(process?.inputStream)
            
            Log.d("LspClient", "LSP Process started: ${command.joinToString(" ")}")
            startListening()
            startErrorListening()
        } catch (e: Exception) {
            Log.e("LspClient", "Failed to start LSP process", e)
        }
    }

    private fun startErrorListening() {
        scope.launch {
            val err = process?.errorStream ?: return@launch
            try {
                val reader = err.bufferedReader()
                var line: String?
                while (isActive) {
                    line = reader.readLine()
                    if (line == null) break
                    Log.e("LspClient", "STDERR: $line")
                }
            } catch (e: Exception) {
                Log.e("LspClient", "Error listening to LSP stderr", e)
            }
        }
    }

    private fun startListening() {
        scope.launch {
            val input = stdout ?: return@launch
            try {
                while (isActive) {
                    val headers = mutableMapOf<String, String>()
                    var line = readLine(input)
                    if (line == null) break
                    
                    while (line!!.isNotEmpty()) {
                        if (line!!.contains(": ")) {
                            val parts = line!!.split(": ", limit = 2)
                            if (parts.size == 2) {
                                headers[parts[0]] = parts[1]
                            }
                        }
                        line = readLine(input)
                        if (line == null) break
                    }
                    if (line == null) break
                    
                    val contentLengthHeader = headers.entries.find { it.key.equals("Content-Length", ignoreCase = true) }
                    val contentLengthStr = contentLengthHeader?.value?.trim() ?: ""
                    val contentLength = contentLengthStr.toIntOrNull()
                    
                    if (contentLength == null) {
                        Log.w("LspClient", "Missing or invalid Content-Length: '$contentLengthStr'")
                        continue
                    }
                    
                    val buffer = ByteArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = input.read(buffer, read, contentLength - read)
                        if (r <= 0) break
                        read += r
                    }
                    
                    if (read == contentLength) {
                        val content = String(buffer, Charsets.UTF_8)
                        handleMessage(content)
                    } else {
                        Log.e("LspClient", "Short read: expected $contentLength, got $read")
                    }
                }
            } catch (e: Exception) {
                Log.e("LspClient", "Error listening to LSP", e)
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) break
            if (c == '\r'.code) {
                // Peek at next byte to see if it's \n
                input.mark(1)
                val next = input.read()
                if (next != '\n'.code && next != -1) {
                    input.reset()
                }
                break
            }
            sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun handleMessage(content: String) {
        try {
            Log.d("LspClient", "RECV: $content")
            val element = JsonParser.parseString(content)
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                
                // Response
                if (obj.has("id") && (obj.has("result") || obj.has("error"))) {
                    val idElement = obj.get("id")
                    val id = if (idElement.isJsonPrimitive) {
                        if (idElement.asJsonPrimitive.isNumber) idElement.asInt else -1
                    } else -1
                    if (id != -1) {
                        pendingRequests.remove(id)?.complete(obj)
                    }
                } 
                // Notification
                else if (obj.has("method")) {
                    val method = obj.get("method").asString
                    if (method == "textDocument/publishDiagnostics") {
                        val params = gson.fromJson(obj.get("params"), PublishDiagnosticsParams::class.java)
                        onDiagnosticsReceived?.invoke(params)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LspClient", "Error handling message: $content", e)
        }
    }

    @Synchronized
    private fun send(message: Any) {
        val json = gson.toJson(message)
        val payload = "Content-Length: ${json.toByteArray().size}\r\n\r\n$json"
        try {
            Log.d("LspClient", "SEND: $json")
            stdin?.write(payload.toByteArray())
            stdin?.flush()
        } catch (e: Exception) {
            Log.e("LspClient", "Failed to send message", e)
        }
    }

    suspend fun request(method: String, params: Any): JsonObject? {
        val id = messageId.getAndIncrement()
        val req = JsonRpcRequest(id = id, method = method, params = params)
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        send(req)
        val res = withTimeoutOrNull(5000) { deferred.await() }
        if (res == null) {
            val alive = process?.isAlive ?: false
            Log.w("LspClient", "Request $id ($method) timed out. Process alive=$alive")
        }
        return res
    }

    fun notify(method: String, params: Any) {
        val notif = JsonRpcNotification(method = method, params = params)
        send(notif)
    }

    fun stop() {
        scope.cancel()
        process?.destroy()
    }
}

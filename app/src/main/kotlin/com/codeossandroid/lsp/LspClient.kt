package com.codeossandroid.lsp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

class LspClient(
    private val command: List<String>,
    private val workingDir: java.io.File,
    private val env: Map<String, String>
) {
    private var process: Process? = null
    private var stdin: OutputStream? = null
    private var stdout: InputStream? = null
    
    private val gson = Gson()
    private val messageId = AtomicInteger(1)
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    var onDiagnosticsReceived: ((PublishDiagnosticsParams) -> Unit)? = null
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<JsonObject>>()

    fun start() {
        val pb = ProcessBuilder(command)
        pb.directory(workingDir)
        pb.environment().putAll(env)
        
        try {
            process = pb.start()
            stdin = process?.outputStream
            stdout = process?.inputStream
            
            Log.d("LspClient", "LSP Process started: ${command.joinToString(" ")}")
            startListening()
        } catch (e: Exception) {
            Log.e("LspClient", "Failed to start LSP process", e)
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
                        val parts = line!!.split(": ", limit = 2)
                        if (parts.size == 2) {
                            headers[parts[0]] = parts[1]
                        }
                        line = readLine(input)
                    }
                    
                    val contentLength = headers["Content-Length"]?.toIntOrNull() ?: continue
                    val buffer = ByteArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = input.read(buffer, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    
                    val content = String(buffer, Charsets.UTF_8)
                    handleMessage(content)
                }
            } catch (e: Exception) {
                Log.e("LspClient", "Error listening to LSP", e)
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var c = input.read()
        if (c == -1) return null
        while (c != -1) {
            if (c == '\r'.code) {
                val next = input.read()
                if (next == '\n'.code) {
                    break
                } else {
                    sb.append('\r')
                    c = next
                    continue
                }
            }
            sb.append(c.toChar())
            c = input.read()
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
                    val id = obj.get("id").asInt
                    pendingRequests.remove(id)?.complete(obj)
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
        return withTimeoutOrNull(5000) { deferred.await() }
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

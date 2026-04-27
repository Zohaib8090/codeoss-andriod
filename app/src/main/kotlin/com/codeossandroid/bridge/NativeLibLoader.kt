package com.codeossandroid.bridge

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object NativeLibLoader {
    private var isInitialized = false

    fun init(context: Context) {
        val logFile = File(context.filesDir, "loader_log.txt")
        logFile.writeText("init() called at ${java.util.Date()}\n")
        Log.i("NativeLibLoader", "init() called")
        if (isInitialized) return
        
        try {
            val libDir = File(context.filesDir, "lib")
            if (!libDir.exists()) libDir.mkdirs()

            // Also patch ALL libraries in the standard library directory
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            logFile.appendText("Processing all libs in $nativeLibDir...\n")
            
            File(nativeLibDir).listFiles { f -> f.name.contains(".so") }?.forEach { srcFile ->
                val name = srcFile.name
                logFile.appendText("Handling $name\n")
                val dstFile = File(libDir, name)
                srcFile.inputStream().use { input ->
                    dstFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val loadOrder = listOf(
                "libc++_shared.so",
                "libiconv1.so",
                "libexpat1.so",
                "libz_9.so",
                "libcare.so",
                "libngh2.so",
                "libngh3.so",
                "libngtp2.so",
                "libngtcp2_crypto_ossl.so",
                "libbrcm.so",
                "libbrdec.so",
                "libbrenc.so",
                "libcrypto_9.so",
                "libssl_9.so",
                "libssh9.so",
                "libcur9.so",
                "libicudata_99.so",
                "libicuuc_99.so",
                "libicui18n_99.so",
                "libsqlite9.so",
                "libpcre28.so",
                "libnode.so",
                "libgit2.so",
                "libgit.so",
                "libgit_remote_http.so",
                "libnative-lib.so"
            )



            
            val loadedSet = mutableSetOf<String>()
            
            loadOrder.forEach { name ->
                val f = File(libDir, name)
                if (f.exists()) {
                    try {
                        System.load(f.absolutePath)
                        logFile.appendText("Loaded $name\n")
                        loadedSet.add(name)
                    } catch (e: Throwable) {
                        logFile.appendText("Failed to load $name: ${e.message}\n")
                    }
                } else {
                    logFile.appendText("File not found to load: $name\n")
                }
            }

            // 2. Try loading the rest (trial and error loops)
            var madeProgress = true
            while (madeProgress) {
                madeProgress = false
                libDir.listFiles { f -> f.name.contains(".so") }?.forEach { f ->
                    if (f.name !in loadedSet) {
                        try {
                            System.load(f.absolutePath)
                            logFile.appendText("Loaded ${f.name}\n")
                            loadedSet.add(f.name)
                            madeProgress = true
                        } catch (e: Throwable) {
                            // ignore for now, will try again if progress is made
                        }
                    }
                }
            }

            isInitialized = true
            logFile.appendText("NativeLibLoader initialized successfully\n")
        } catch (e: Throwable) {
            logFile.appendText("CRITICAL ERROR: ${e.message}\n")
            logFile.appendText(e.stackTraceToString() + "\n")
        }
    }
}

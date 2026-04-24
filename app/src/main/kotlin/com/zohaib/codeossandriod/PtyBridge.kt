package com.zohaib.codeossandriod

import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Field

class PtyBridge {
    private var masterFd: Int = -1
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun createPty(shell: String, rows: Int, cols: Int): Int
    external fun setWindowSize(fd: Int, rows: Int, cols: Int)

    fun startShell(shell: String = "/system/bin/sh", rows: Int = 24, cols: Int = 80) {
        masterFd = createPty(shell, rows, cols)
        if (masterFd != -1) {
            val fd = FileDescriptor()
            try {
                val field: Field = FileDescriptor::class.java.getDeclaredField("descriptor")
                field.isAccessible = true
                field.setInt(fd, masterFd)
                
                inputStream = FileInputStream(fd)
                outputStream = FileOutputStream(fd)
                
                Log.i("PtyBridge", "Shell started with FD: $masterFd")
            } catch (e: Exception) {
                Log.e("PtyBridge", "Failed to create streams", e)
            }
        }
    }

    fun write(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("PtyBridge", "Write failed", e)
        }
    }

    fun read(buffer: ByteArray): Int {
        return try {
            inputStream?.read(buffer) ?: -1
        } catch (e: IOException) {
            Log.e("PtyBridge", "Read failed", e)
            -1
        }
    }

    fun resize(rows: Int, cols: Int) {
        if (masterFd != -1) {
            setWindowSize(masterFd, rows, cols)
        }
    }
}

package com.codeossandroid

import android.app.Application
import android.content.Context
import com.codeossandroid.bridge.NativeLibLoader

class CodeOSSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("CodeOSSApp", "Application onCreate - initializing NativeLibLoader")
        NativeLibLoader.init(this)
    }
}

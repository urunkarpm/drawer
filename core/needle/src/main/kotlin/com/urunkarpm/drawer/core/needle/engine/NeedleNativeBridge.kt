package com.urunkarpm.drawer.core.needle.engine

import android.util.Log

object NeedleNativeBridge {
    private const val TAG = "NeedleNativeBridge"

    val isNativeLoaded: Boolean = try {
        System.loadLibrary("needle_jni")
        Log.i(TAG, "Native library 'needle_jni' loaded successfully")
        true
    } catch (e: Throwable) {
        Log.w(TAG, "Native library 'needle_jni' not available: ${e.message}")
        false
    }

    fun isAvailable(): Boolean = isNativeLoaded

    fun loadModel(path: String): Boolean {
        if (!isNativeLoaded) return false
        return try {
            nativeLoad(path)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load model from $path: ${e.message}", e)
            false
        }
    }

    fun init(systemPrompt: String, toolsJson: String): Boolean {
        if (!isNativeLoaded) return false
        return try {
            nativeInit(systemPrompt, toolsJson)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to init session: ${e.message}", e)
            false
        }
    }

    fun complete(prompt: String, maxTokens: Int = 128): String? {
        if (!isNativeLoaded) return null
        return try {
            nativeComplete(prompt, maxTokens)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to complete prompt: ${e.message}", e)
            null
        }
    }

    fun reset() {
        if (!isNativeLoaded) return
        try {
            nativeReset()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to reset session: ${e.message}")
        }
    }

    fun getLastError(): String {
        if (!isNativeLoaded) return "Native library not loaded"
        return try {
            nativeGetLastError() ?: ""
        } catch (e: Throwable) {
            e.message ?: "Unknown error"
        }
    }

    private external fun nativeLoad(modelPath: String): Boolean
    private external fun nativeInit(systemPrompt: String, toolsJson: String): Boolean
    private external fun nativeComplete(prompt: String, maxTokens: Int): String?
    private external fun nativeReset()
    private external fun nativeGetLastError(): String?
}

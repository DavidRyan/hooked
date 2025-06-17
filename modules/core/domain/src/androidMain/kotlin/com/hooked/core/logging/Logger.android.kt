package com.hooked.core.logging

import android.util.Log

actual object Logger {
    actual fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    actual fun debug(tag: String, message: String, throwable: Throwable) {
        Log.d(tag, message, throwable)
    }
    
    actual fun info(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    actual fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    actual fun warning(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }
    
    actual fun error(tag: String, message: String) {
        Log.e(tag, message)
    }
    
    actual fun error(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}
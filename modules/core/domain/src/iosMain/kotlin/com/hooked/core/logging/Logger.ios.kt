package com.hooked.core.logging

import platform.Foundation.NSLog

actual object Logger {
    actual fun debug(tag: String, message: String) {
        NSLog("DEBUG [$tag]: $message")
    }
    
    actual fun debug(tag: String, message: String, throwable: Throwable) {
        NSLog("DEBUG [$tag]: $message - ${throwable.message}")
    }
    
    actual fun info(tag: String, message: String) {
        NSLog("INFO [$tag]: $message")
    }
    
    actual fun warning(tag: String, message: String) {
        NSLog("WARNING [$tag]: $message")
    }
    
    actual fun warning(tag: String, message: String, throwable: Throwable) {
        NSLog("WARNING [$tag]: $message - ${throwable.message}")
    }
    
    actual fun error(tag: String, message: String) {
        NSLog("ERROR [$tag]: $message")
    }
    
    actual fun error(tag: String, message: String, throwable: Throwable) {
        NSLog("ERROR [$tag]: $message - ${throwable.message}")
    }
}
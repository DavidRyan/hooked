package com.hooked.hooked

import android.app.Application
import com.hooked.core.logging.Logger
import com.hooked.core.logging.LogLevel

class HookedApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure logging
        // In production, you can set minLogLevel to ERROR to suppress debug/info logs
        Logger.configure(
            isEnabled = true,
            minLogLevel = LogLevel.DEBUG,  // Change to LogLevel.ERROR for production
            appTag = "Hooked"
        )
    }
}

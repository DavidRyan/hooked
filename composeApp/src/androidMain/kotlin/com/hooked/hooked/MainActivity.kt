package com.hooked.hooked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import di.initKoin
import com.hooked.HookedApp
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = this.application
        initKoin {
            androidContext(application)
        }
        setContent {
            HookedApp()
        }
    }
}

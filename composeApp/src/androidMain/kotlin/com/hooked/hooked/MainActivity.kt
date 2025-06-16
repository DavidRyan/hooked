package com.hooked.hooked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hooked.app.di.initKoin
import com.hooked.app.HookedApp
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

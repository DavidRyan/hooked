package com.hooked.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import com.hooked.app.HookedApp
import com.hooked.app.di.initKoin

fun MainViewController() = ComposeUIViewController { 
    initKoin()
    HookedApp() 
}
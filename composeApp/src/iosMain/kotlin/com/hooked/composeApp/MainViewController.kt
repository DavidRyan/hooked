package com.hooked.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import com.hooked.core.HookedApp
import com.hooked.di.initKoin

fun MainViewController() = ComposeUIViewController { 
    initKoin()
    HookedApp() 
}
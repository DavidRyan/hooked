package com.hooked.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import com.hooked.HookedApp
import di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    HookedApp()
}
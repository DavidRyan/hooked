package com.hooked.catches.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hooked.catches.presentation.CatchesScreenState

@Composable
fun rememberCatchesScreenState(): CatchesScreenStateManager {
    return remember { CatchesScreenStateManager() }
}

class CatchesScreenStateManager {
    var screenState by mutableStateOf<CatchesScreenState>(CatchesScreenState.Grid)
        private set
    
    var animationKey by mutableStateOf(0)
        private set
    
    fun navigateToDetails(catchId: String) {
        animationKey += 1
        screenState = CatchesScreenState.Details(catchId)
    }
    
    fun navigateToGrid() {
        screenState = CatchesScreenState.Grid
    }
    
    fun getCurrentCatchId(): String? {
        return when (val state = screenState) {
            is CatchesScreenState.Details -> state.catchId
            else -> null
        }
    }
}
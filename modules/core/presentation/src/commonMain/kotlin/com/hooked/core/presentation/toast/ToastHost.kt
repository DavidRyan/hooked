package com.hooked.core.presentation.toast

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun ToastHost(
    modifier: Modifier = Modifier,
    toastManager: ToastManager = koinInject()
) {
    val toasts by toastManager.toasts.collectAsState()
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toasts.forEach { toast ->
                key(toast.id) {
                    ToastItem(
                        toast = toast,
                        onDismiss = { toastManager.dismissToast(toast.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToastItem(
    toast: ToastMessage,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(toast.id) {
        visible = true
        delay(toast.duration)
        visible = false
        delay(300) // Animation duration
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(
                containerColor = when (toast.type) {
                    ToastType.SUCCESS -> Color(0xFF4CAF50)
                    ToastType.ERROR -> Color(0xFFF44336)
                    ToastType.WARNING -> Color(0xFFFF9800)
                    ToastType.INFO -> Color(0xFF2196F3)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = toast.message,
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
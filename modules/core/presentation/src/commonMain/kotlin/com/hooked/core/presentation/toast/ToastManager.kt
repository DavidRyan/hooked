package com.hooked.core.presentation.toast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToastManager {
    private val _toasts = MutableStateFlow<List<ToastMessage>>(emptyList())
    val toasts: StateFlow<List<ToastMessage>> = _toasts.asStateFlow()
    
    fun showToast(message: String, type: ToastType = ToastType.INFO, duration: Long = 3000L) {
        val toast = ToastMessage(
            message = message,
            type = type,
            duration = duration
        )
        _toasts.value = _toasts.value + toast
    }
    
    fun showError(message: String) {
        showToast(message, ToastType.ERROR, 4000L) // Longer duration for errors
    }
    
    fun showSuccess(message: String) {
        showToast(message, ToastType.SUCCESS)
    }
    
    fun showWarning(message: String) {
        showToast(message, ToastType.WARNING)
    }
    
    fun showInfo(message: String) {
        showToast(message, ToastType.INFO)
    }
    
    fun dismissToast(toastId: String) {
        _toasts.value = _toasts.value.filter { it.id != toastId }
    }
    
    fun clearAll() {
        _toasts.value = emptyList()
    }
}
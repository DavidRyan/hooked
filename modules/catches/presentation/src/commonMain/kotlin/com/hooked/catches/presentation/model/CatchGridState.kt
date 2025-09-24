package com.hooked.catches.presentation.model

data class CatchGridState(
    val catches: List<CatchModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

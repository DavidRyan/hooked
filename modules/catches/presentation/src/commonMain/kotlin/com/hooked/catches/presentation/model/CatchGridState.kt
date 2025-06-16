package com.hooked.features.catches.presentation.model

data class CatchGridState(
    val catches: List<CatchModel> = emptyList(),
    val isLoading: Boolean = true
)

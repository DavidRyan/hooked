package com.hooked.domain

data class CatchGridState(
    val catches: List<CatchModel> = emptyList(),
    val isLoading: Boolean = true
)

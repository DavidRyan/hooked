package com.hooked.auth.domain.entities

data class OnboardingPreferences(
    val homeLat: Double? = null,
    val homeLng: Double? = null,
    val targetSpecies: List<String>? = null,
    val onboardingCompleted: Boolean? = null
)

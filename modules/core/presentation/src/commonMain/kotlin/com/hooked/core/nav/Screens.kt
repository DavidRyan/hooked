package com.hooked.core.nav

import kotlinx.serialization.Serializable

sealed class Screens() {
    @Serializable
    object Login : Screens()
    @Serializable
    object CreateAccount : Screens()
    @Serializable
    object Onboarding : Screens()
    @Serializable
    object CatchGrid : Screens()
    @Serializable
    data class CatchDetails(val catchId: String) : Screens()
    @Serializable
    object SubmitCatch : Screens()
    @Serializable
    object SubmitSkunk : Screens()
    @Serializable
    object Map : Screens()
    @Serializable
    data class Chat(val starterPrompt: String? = null) : Screens()
    @Serializable
    object Insights : Screens()
    @Serializable
    object Profile : Screens()
    @Serializable
    object AnimationTest : Screens()

    companion object {
        // Top-level destinations that show the bottom navigation bar.
        // Stored as qualified names so we can compare against NavController routes.
        val topLevelRoutes: Set<String> = setOf(
            CatchGrid::class.qualifiedName!!,
            Map::class.qualifiedName!!,
            Chat::class.qualifiedName!!,
            Insights::class.qualifiedName!!,
            Profile::class.qualifiedName!!
        )
    }
}

package com.hooked.core.nav

import kotlinx.serialization.Serializable

sealed class Screens() {
    @Serializable
    object Login : Screens()
    @Serializable
    object CreateAccount : Screens()
    @Serializable
    object CatchGrid : Screens()
    @Serializable
    data class CatchDetails(val catchId: String) : Screens()
    @Serializable
    object SubmitCatch : Screens()
    @Serializable
    object SubmitSkunk : Screens()
    @Serializable
    object Stats : Screens()
    @Serializable
    object AnimationTest : Screens()
}
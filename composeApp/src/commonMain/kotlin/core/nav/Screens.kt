package core.nav

import kotlinx.serialization.Serializable

sealed class Screens() {
    @Serializable
    object CatchGrid : Screens()
    @Serializable
    data class CatchDetails(val catchId: Long) : Screens()
    @Serializable
    object SubmitCatch : Screens()
}
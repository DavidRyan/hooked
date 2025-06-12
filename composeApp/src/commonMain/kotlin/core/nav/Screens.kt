package core.nav

sealed class Screens(val route: String) {
    object CatchGrid : Screens("catchGrid")
    data class CatchDetails(val catchId: String) : Screens("catchDetails/{catchId}") {
        fun createRoute(catchId: String) = "catchDetails/$catchId"
    }
}
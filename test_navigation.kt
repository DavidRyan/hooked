// Simple test to verify navigation imports work
import com.hooked.auth.presentation.LoginScreen
import com.hooked.core.nav.Screens

fun testNavigation() {
    // Test that we can reference the Login screen
    val loginScreen = Screens.Login
    val catchGrid = Screens.CatchGrid
    
    println("Navigation setup complete: Login -> CatchGrid")
}
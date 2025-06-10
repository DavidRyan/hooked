import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import screen.CatchGridScreen  // your shared Compose entry

fun MainViewController() = ComposeUIViewController {
    App()
}

@Composable
fun App() {
    CatchGridScreen() // or whatever your root composable is
}
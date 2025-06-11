package app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import grid.CatchGrid
import grid.CatchGridViewModel
import theme.HookedTheme

@Composable
fun App() {
    HookedTheme {
        CatchGrid(
            modifier = Modifier,
            CatchGridViewModel())
    }
}
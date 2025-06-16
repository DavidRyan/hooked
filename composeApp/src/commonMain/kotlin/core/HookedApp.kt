package core

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import details.CatchDetailsScreen
import details.CatchDetailsViewModel
import grid.CatchGridScreen
import grid.CatchGridViewModel
import submit.SubmitCatchScreen
import submit.SubmitCatchViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import theme.HookedTheme
import core.nav.Screens

@Composable
fun HookedApp(
) {
    HookedTheme {
        KoinContext {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screens.CatchGrid
            ) {
                composable<Screens.CatchGrid> {
                    CatchGridScreen(
                        viewModel = koinViewModel<CatchGridViewModel>(),
                        navigate = { screen ->
                            navController.navigate(screen)
                        }
                    )
                }
                composable<Screens.CatchDetails>(
                ) { backStackEntry ->
                    val details = backStackEntry.toRoute<Screens.CatchDetails>()
                    CatchDetailsScreen(
                        viewModel = koinViewModel<CatchDetailsViewModel>(),
                        catchId = details.catchId
                    )
                }
                composable<Screens.SubmitCatch> {
                    SubmitCatchScreen(
                        viewModel = koinViewModel<SubmitCatchViewModel>(),
                        navigate = { screen ->
                            navController.navigate(screen)
                        }
                    )
                }
            }
        }
    }
}

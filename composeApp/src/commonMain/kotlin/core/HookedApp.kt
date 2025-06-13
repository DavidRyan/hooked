package core

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import core.nav.Screens
import details.CatchDetailsScreen
import details.CatchDetailsViewModel
import grid.CatchGridScreen
import grid.CatchGridViewModel
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import theme.HookedTheme

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
            }
        }
    }
}

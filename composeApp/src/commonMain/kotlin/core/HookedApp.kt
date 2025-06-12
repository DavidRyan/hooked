package core

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import core.nav.Screens
import grid.CatchGridScreen
import grid.CatchGridViewModel
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import theme.HookedTheme

@Composable
fun HookedApp() {
    HookedTheme {
        KoinContext {
            NavHost(
                navController = rememberNavController(),
                startDestination = Screens.CatchGrid.route
            ) {
                composable(route = Screens.CatchGrid.route) {
                    CatchGridScreen(
                        viewModel = koinViewModel<CatchGridViewModel>()
                    )
                }
                composable(
                    route = Screens.CatchDetails("{catchId}").route,
                    arguments = listOf(navArgument("catchId") { type = NavType.StringType })
                ) { backStackEntry ->
                    //val catchId = backStackEntry.arguments?.getString("catchId") ?: return@composable
                    //CatchDetailsScreen(catchId, koinViewModel<CatchDetailsViewModel>()
                }
            }
        }
    }
}
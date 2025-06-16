package com.hooked

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hooked.catches.presentation.CatchDetailsScreen
import com.hooked.catches.presentation.CatchDetailsViewModel
import com.hooked.catches.presentation.CatchGridScreen
import com.hooked.catches.presentation.CatchGridViewModel
import com.hooked.submit.presentation.SubmitCatchScreen
import com.hooked.submit.presentation.SubmitCatchViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import com.hooked.theme.HookedTheme
import com.hooked.core.nav.Screens

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

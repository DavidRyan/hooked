package com.hooked

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hooked.catches.presentation.CatchesScreen
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
                    CatchesScreen(
                        navigate = { screen ->
                            navController.navigate(screen)
                        }
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

package com.hooked

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                startDestination = Screens.CatchGrid,
                modifier = Modifier.background(HookedTheme.background)
            ) {
                composable<Screens.CatchGrid>(
                    exitTransition = {
                        when (targetState.destination.route) {
                            Screens.SubmitCatch::class.qualifiedName -> {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    },
                    popEnterTransition = {
                        when (initialState.destination.route) {
                            Screens.SubmitCatch::class.qualifiedName -> {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    }
                ) {
                    CatchesScreen(
                        navigate = { screen ->
                            navController.navigate(screen)
                        }
                    )
                }
                composable<Screens.SubmitCatch>(
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    }
                ) {
                    SubmitCatchScreen(
                        viewModel = koinViewModel<SubmitCatchViewModel>(),
                        navigate = { screen ->
                            when (screen) {
                                is Screens.CatchGrid -> navController.popBackStack()
                                else -> navController.navigate(screen)
                            }
                        }
                    )
                }
            }
        }
    }
}

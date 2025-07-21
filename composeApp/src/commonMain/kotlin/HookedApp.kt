package com.hooked

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hooked.auth.presentation.LoginScreen
import com.hooked.auth.presentation.CreateAccountScreen
import com.hooked.catches.presentation.CatchesScreen
import com.hooked.catches.presentation.SubmitCatchScreen
import com.hooked.catches.presentation.SubmitCatchViewModel
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
            var startDestination by remember { mutableStateOf<Screens?>(null) }
            
            // Check auth status and set initial destination
            AuthStateManager(
                onAuthenticatedUser = { 
                    startDestination = Screens.CatchGrid 
                },
                onUnauthenticatedUser = { 
                    startDestination = Screens.Login 
                }
            )
            
            // Only show NavHost once we know the start destination
            startDestination?.let { destination ->
                NavHost(
                    navController = navController,
                    startDestination = destination,
                    modifier = Modifier.background(HookedTheme.background)
                ) {
                composable<Screens.Login>(
                    exitTransition = {
                        when (targetState.destination.route) {
                            Screens.CatchGrid::class.qualifiedName -> {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            }
                            Screens.CreateAccount::class.qualifiedName -> {
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
                            Screens.CreateAccount::class.qualifiedName -> {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    }
                ) {
                    LoginScreen(
                        onNavigateToHome = {
                            navController.navigate(Screens.CatchGrid) {
                                popUpTo(Screens.Login) { inclusive = true }
                            }
                        },
                        onNavigateToCreateAccount = {
                            navController.navigate(Screens.CreateAccount)
                        }
                    )
                }
                composable<Screens.CreateAccount>(
                    enterTransition = {
                        when (initialState.destination.route) {
                            Screens.Login::class.qualifiedName -> {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    },
                    exitTransition = {
                        when (targetState.destination.route) {
                            Screens.CatchGrid::class.qualifiedName -> {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            }
                            Screens.Login::class.qualifiedName -> {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    }
                ) {
                    CreateAccountScreen(
                        onNavigateToHome = {
                            navController.navigate(Screens.CatchGrid) {
                                popUpTo(Screens.Login) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Screens.CatchGrid>(
                    enterTransition = {
                        when (initialState.destination.route) {
                            Screens.Login::class.qualifiedName -> {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            }
                            else -> null
                        }
                    },
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
}

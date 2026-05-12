package com.hooked

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.hooked.auth.presentation.OnboardingScreen
import com.hooked.auth.presentation.ProfileScreen
import com.hooked.chat.ChatScreen
import com.hooked.catches.presentation.AnimationTestScreen
import com.hooked.catches.presentation.CatchesScreen
import com.hooked.core.presentation.toast.ToastHost
import com.hooked.catches.presentation.SubmitCatchScreen
import com.hooked.catches.presentation.SubmitCatchViewModel
import com.hooked.catches.presentation.StatsScreen
import com.hooked.skunks.presentation.SubmitSkunkScreen
import com.hooked.skunks.presentation.SubmitSkunkViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import com.hooked.theme.HookedTheme
import com.hooked.core.nav.Screens

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HookedApp(
) {
    HookedTheme {
        KoinContext {
            val navController = rememberNavController()
            var startDestination by remember { mutableStateOf<Screens?>(null) }
            
            // Check auth status and set initial destination
            AuthStateManager(
                onAuthenticatedAndOnboarded = {
                    startDestination = Screens.CatchGrid
                },
                onAuthenticatedNeedsOnboarding = {
                    startDestination = Screens.Onboarding
                },
                onUnauthenticatedUser = {
                    startDestination = Screens.Login
                }
            )
            
            // Only show NavHost once we know the start destination
            startDestination?.let { destination ->
                SharedTransitionLayout {
                    androidx.compose.material3.Scaffold(
                        containerColor = HookedTheme.background,
                        bottomBar = { HookedBottomBar(navController) }
                    ) { scaffoldPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                            navController = navController,
                            startDestination = destination,
                            modifier = Modifier
                                .background(HookedTheme.background)
                                .padding(scaffoldPadding)
                        ) {
                composable<Screens.AnimationTest> {
                    AnimationTestScreen()
                }
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
                        onNavigateToOnboarding = {
                            navController.navigate(Screens.Onboarding) {
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
                            navController.navigate(Screens.Onboarding) {
                                popUpTo(Screens.Login) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Screens.Onboarding> {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(Screens.CatchGrid) {
                                popUpTo(Screens.Onboarding) { inclusive = true }
                            }
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
                            Screens.SubmitCatch::class.qualifiedName,
                            Screens.SubmitSkunk::class.qualifiedName,
                            Screens.Profile::class.qualifiedName -> {
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
                            Screens.SubmitCatch::class.qualifiedName,
                            Screens.SubmitSkunk::class.qualifiedName,
                            Screens.Profile::class.qualifiedName -> {
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
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
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
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                     )
                     }
                composable<Screens.SubmitSkunk>(
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
                    SubmitSkunkScreen(
                        viewModel = koinViewModel<SubmitSkunkViewModel>(),
                        navigate = { screen ->
                            when (screen) {
                                is Screens.CatchGrid -> navController.popBackStack()
                                else -> navController.navigate(screen)
                            }
                        }
                    )
                }
                 composable<Screens.Insights> {
                     StatsScreen(
                         onNavigateBack = { navController.popBackStack() }
                     )
                 }
                 composable<Screens.Map> {
                     com.hooked.catches.presentation.MapScreen()
                 }
                 composable<Screens.Chat> { entry ->
                     val chatArgs: Screens.Chat = entry.toRoute()
                     ChatScreen(starterPrompt = chatArgs.starterPrompt)
                 }
                composable<Screens.Profile>(
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
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    }
                ) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToLogin = {
                            navController.navigate(Screens.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                     }

                         // Toast overlay
                         ToastHost()
                         }
                     }
                 }
             }
         }
     }
}

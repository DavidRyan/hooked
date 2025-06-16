package com.hooked.features.submit

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hooked.core.feature.FeatureApi
import com.hooked.core.nav.Screens
import com.hooked.features.submit.di.submitModule
import com.hooked.features.submit.presentation.SubmitCatchScreen
import com.hooked.features.submit.presentation.SubmitCatchViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module

/**
 * Submit feature provides catch submission functionality
 */
object SubmitFeatureApi : FeatureApi {
    override val featureId: String = "submit"
    
    override val koinModule: Module = submitModule
    
    override fun registerNavigation(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.apply {
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
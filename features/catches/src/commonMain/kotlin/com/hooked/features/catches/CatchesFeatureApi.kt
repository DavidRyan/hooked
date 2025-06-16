package com.hooked.features.catches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.hooked.core.feature.FeatureApi
import com.hooked.core.feature.FloatingActionProvider
import com.hooked.core.nav.Screens
import com.hooked.features.catches.di.catchesModule
import com.hooked.features.catches.presentation.CatchDetailsScreen
import com.hooked.features.catches.presentation.CatchDetailsViewModel
import com.hooked.features.catches.presentation.CatchGridScreen
import com.hooked.features.catches.presentation.CatchGridViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module

/**
 * Catches feature provides catch grid and details functionality
 */
object CatchesFeatureApi : FeatureApi, FloatingActionProvider {
    override val featureId: String = "catches"
    
    override val koinModule: Module = catchesModule
    
    override fun registerNavigation(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.apply {
            composable<Screens.CatchGrid> {
                CatchGridScreen(
                    viewModel = koinViewModel<CatchGridViewModel>(),
                    navigate = { screen ->
                        navController.navigate(screen)
                    }
                )
            }
            
            composable<Screens.CatchDetails> { backStackEntry ->
                val details = backStackEntry.toRoute<Screens.CatchDetails>()
                CatchDetailsScreen(
                    viewModel = koinViewModel<CatchDetailsViewModel>(),
                    catchId = details.catchId
                )
            }
        }
    }
    
    @Composable
    override fun ProvideFab(navController: NavHostController) {
        FloatingActionButton(
            onClick = { navController.navigate(Screens.SubmitCatch) },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new catch"
            )
        }
    }
}
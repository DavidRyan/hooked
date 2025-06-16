package com.hooked.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.hooked.core.feature.FeatureRegistry
import com.hooked.core.feature.FloatingActionProvider
import com.hooked.core.nav.Screens
import com.hooked.features.catches.CatchesFeatureApi
import com.hooked.theme.HookedTheme
import org.koin.compose.KoinContext

@Composable
fun HookedApp() {
    HookedTheme {
        KoinContext {
            val navController = rememberNavController()
            
            Scaffold(
                floatingActionButton = {
                    // Get FAB from catches feature if available
                    val catchesFeature = FeatureRegistry.get("catches") as? FloatingActionProvider
                    catchesFeature?.ProvideFab(navController)
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screens.CatchGrid
                    ) {
                        // Register navigation from all features
                        FeatureRegistry.features.values.forEach { feature ->
                            feature.registerNavigation(this, navController)
                        }
                    }
                }
            }
        }
    }
}
package com.hooked.core

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.hooked.core.di.coreModule
import com.hooked.core.feature.FeatureApi
import org.koin.core.module.Module

/**
 * Core feature provides base functionality needed by all other features
 */
object CoreFeatureApi : FeatureApi {
    override val featureId: String = "core"
    
    override val koinModule: Module = coreModule
    
    override fun registerNavigation(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        // Core doesn't provide navigation destinations
        // It provides the foundation for other features
    }
    
    override fun initialize() {
        // Core initialization if needed
    }
}
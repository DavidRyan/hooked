package com.hooked.app.di

import com.hooked.core.CoreFeatureApi
import com.hooked.core.feature.FeatureRegistry
import com.hooked.features.catches.CatchesFeatureApi
import com.hooked.features.submit.SubmitFeatureApi
import di.dataModule
import di.useCaseModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    // Register all features
    FeatureRegistry.register(CoreFeatureApi)
    FeatureRegistry.register(CatchesFeatureApi)
    FeatureRegistry.register(SubmitFeatureApi)
    
    startKoin {
        config?.invoke(this)
        
        // Include modules from shared layer
        modules(dataModule, useCaseModule)
        
        // Include all feature modules
        modules(FeatureRegistry.getAllModules())
    }
}
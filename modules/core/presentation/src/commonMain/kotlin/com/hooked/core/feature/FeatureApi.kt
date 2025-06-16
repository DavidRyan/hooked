package com.hooked.core.feature

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import org.koin.core.module.Module

/**
 * Base interface for all feature modules
 * Defines the contract that each feature must implement
 */
interface FeatureApi {
    /**
     * Unique identifier for this feature
     */
    val featureId: String
    
    /**
     * Koin module providing this feature's dependencies
     */
    val koinModule: Module
    
    /**
     * Register navigation destinations for this feature
     */
    fun registerNavigation(navGraphBuilder: NavGraphBuilder, navController: NavHostController)
    
    /**
     * Optional initialization logic for the feature
     */
    fun initialize() {}
}

/**
 * Registry for managing all feature modules
 */
object FeatureRegistry {
    private val _features = mutableMapOf<String, FeatureApi>()
    
    val features: Map<String, FeatureApi> get() = _features.toMap()
    
    fun register(feature: FeatureApi) {
        _features[feature.featureId] = feature
        feature.initialize()
    }
    
    fun get(featureId: String): FeatureApi? = _features[featureId]
    
    fun getAllModules(): List<Module> = _features.values.map { it.koinModule }
}
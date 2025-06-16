package com.hooked.core.feature

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Interface for features that provide composable destinations
 */
interface NavigationDestination {
    val route: String
    
    @Composable
    fun Content(navController: NavHostController)
}

/**
 * Interface for features that provide floating action buttons
 */
interface FloatingActionProvider {
    @Composable
    fun ProvideFab(navController: NavHostController)
}

/**
 * Interface for features that can be launched from other features
 */
interface LaunchableFeature {
    fun canHandle(route: String): Boolean
    fun createLaunchIntent(route: String, params: Map<String, Any> = emptyMap()): Any
}
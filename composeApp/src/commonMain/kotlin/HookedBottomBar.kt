package com.hooked

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hooked.core.nav.Screens
import com.hooked.theme.Colors

private data class BottomTab(
    val screen: Screens,
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    BottomTab(Screens.CatchGrid, Screens.CatchGrid::class.qualifiedName!!, "Log", Icons.AutoMirrored.Filled.List),
    BottomTab(Screens.Map, Screens.Map::class.qualifiedName!!, "Map", Icons.Filled.LocationOn),
    BottomTab(Screens.Insights, Screens.Insights::class.qualifiedName!!, "Insights", Icons.Filled.Info),
    BottomTab(Screens.Profile, Screens.Profile::class.qualifiedName!!, "Profile", Icons.Filled.Person)
)

@Composable
fun HookedBottomBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    if (currentRoute !in Screens.topLevelRoutes) return

    NavigationBar(
        containerColor = Colors.surface0,
        contentColor = Colors.text
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.screen) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Colors.primary,
                    selectedTextColor = Colors.primary,
                    indicatorColor = Colors.primary.copy(alpha = 0.15f),
                    unselectedIconColor = Colors.subtext1,
                    unselectedTextColor = Colors.subtext1
                )
            )
        }
    }
}

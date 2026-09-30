package ru.kinopolka.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** Root composable: bottom navigation bar and the navigation host. */
@Composable
fun KinopolkaApp(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.topLevelDestination()

    // Details, genre and about screens live outside the tab graphs: the tab from which
    // they were opened stays selected.
    var lastTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    LaunchedEffect(currentTab) {
        if (currentTab != null) lastTab = currentTab
    }
    val selectedTab = currentTab ?: lastTab

    Scaffold(
        modifier = modifier,
        // Screens draw their own top app bars under the status bar (edge-to-edge).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selectedTab,
                        onClick = { navController.navigateToTab(destination) },
                        icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        KinopolkaNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { destination ->
        hierarchy.any { it.isGraphOf(destination) }
    }

private fun NavDestination.isGraphOf(destination: TopLevelDestination): Boolean = when (destination) {
    TopLevelDestination.HOME -> hasRoute<HomeGraph>()
    TopLevelDestination.SEARCH -> hasRoute<SearchGraph>()
    TopLevelDestination.LIBRARY -> hasRoute<LibraryGraph>()
}

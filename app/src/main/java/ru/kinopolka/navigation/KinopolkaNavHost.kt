package ru.kinopolka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.kinopolka.feature.about.AboutScreen
import ru.kinopolka.feature.details.DetailsScreen
import ru.kinopolka.feature.genre.GenreScreen
import ru.kinopolka.feature.home.HomeScreen
import ru.kinopolka.feature.library.LibraryScreen
import ru.kinopolka.feature.search.SearchScreen

@Composable
fun KinopolkaNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeGraph,
        modifier = modifier,
    ) {
        navigation<HomeGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenSearch = { navController.navigateToTab(TopLevelDestination.SEARCH) },
                    onOpenGenre = { genre, filter -> navController.navigate(GenreRoute(genre.key, filter)) },
                )
            }
        }
        navigation<SearchGraph>(startDestination = SearchRoute) {
            composable<SearchRoute> {
                SearchScreen()
            }
        }
        navigation<LibraryGraph>(startDestination = LibraryRoute) {
            composable<LibraryRoute> {
                LibraryScreen(onOpenAbout = { navController.navigate(AboutRoute) })
            }
        }
        composable<DetailsRoute> { entry ->
            DetailsScreen(
                route = entry.toRoute(),
                onBack = navController::navigateUp,
            )
        }
        composable<GenreRoute> {
            GenreScreen(onBack = navController::navigateUp)
        }
        composable<AboutRoute> {
            AboutScreen(onBack = navController::navigateUp)
        }
    }
}

/**
 * Switches tabs keeping a separate back stack for each: the stack of the tab being left is
 * saved, the stack of the selected tab is restored.
 */
fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.graph) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

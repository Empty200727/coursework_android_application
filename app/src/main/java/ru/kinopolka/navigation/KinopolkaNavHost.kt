package ru.kinopolka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.feature.about.AboutScreen
import ru.kinopolka.feature.details.DetailsScreen
import ru.kinopolka.feature.genre.GenreScreen
import ru.kinopolka.feature.home.HomeScreen
import ru.kinopolka.feature.library.LibraryScreen
import ru.kinopolka.feature.search.SearchScreen

@Composable
fun KinopolkaNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    // Set by the search field of the home screen: the search screen then focuses its input.
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    val openMedia: (MediaKey) -> Unit = { navController.navigate(DetailsRoute(it)) }
    NavHost(
        navController = navController,
        startDestination = HomeGraph,
        modifier = modifier,
    ) {
        navigation<HomeGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenSearch = {
                        focusSearch = true
                        navController.navigateToTab(TopLevelDestination.SEARCH)
                    },
                    onOpenGenre = { genre, filter -> navController.navigate(GenreRoute(genre.key, filter)) },
                    onOpenMedia = openMedia,
                )
            }
        }
        navigation<SearchGraph>(startDestination = SearchRoute) {
            composable<SearchRoute> {
                SearchScreen(
                    requestFocus = focusSearch,
                    onFocusRequested = { focusSearch = false },
                    onOpenMedia = openMedia,
                )
            }
        }
        navigation<LibraryGraph>(startDestination = LibraryRoute) {
            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onOpenMedia = openMedia,
                    onFindSomething = {
                        focusSearch = true
                        navController.navigateToTab(TopLevelDestination.SEARCH)
                    },
                )
            }
        }
        composable<DetailsRoute> {
            DetailsScreen(onBack = navController::navigateUp, onOpenMedia = openMedia)
        }
        composable<GenreRoute> {
            GenreScreen(onBack = navController::navigateUp, onOpenMedia = openMedia)
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

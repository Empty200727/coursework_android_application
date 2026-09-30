package ru.kinopolka.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.core.ui.theme.KinopolkaTheme
import ru.kinopolka.feature.genre.GenreScreen
import ru.kinopolka.feature.genre.GenreUiState
import ru.kinopolka.feature.home.HomeScreen
import ru.kinopolka.feature.home.HomeUiState
import ru.kinopolka.feature.home.ShelfContent
import ru.kinopolka.feature.search.SearchScreen
import ru.kinopolka.feature.search.SearchUiState

/** Renders the catalog screens with sample data: layout, texts and clicks (without a device). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ScreensSmokeTest {

    // The v2 rule queues coroutines on StandardTestDispatcher and Paging never delivers its
    // items under Robolectric; the immediate dispatcher of the v1 rule suits these smoke tests.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createComposeRule()

    private val action = Genre(key = "action", name = "Боевик", movieGenreId = 28, tvGenreId = 10759)
    private val breakingBad = Media(
        key = MediaKey(MediaType.TV, 1396),
        title = "Во все тяжкие",
        originalTitle = "Breaking Bad",
        overview = null,
        posterPath = null,
        backdropPath = null,
        releaseDate = LocalDate.of(2008, 1, 20),
        voteAverage = 8.9,
        voteCount = 15432,
        popularity = 312.5,
        genreIds = listOf(18, 80),
    )
    private val matrix = breakingBad.copy(
        key = MediaKey(MediaType.MOVIE, 603),
        title = "Матрица",
        genreIds = listOf(28),
    )

    @Test
    fun homeShowsShelvesAndOpensGenreAndTitle() {
        var openedGenre: Genre? = null
        var openedMedia: MediaKey? = null
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                HomeScreen(
                    uiState = HomeUiState(
                        shelves = listOf(
                            ShelfContent(Shelf.Trending(MediaFilter.ALL), listOf(breakingBad)),
                            ShelfContent(Shelf.ByGenre(action, MediaFilter.ALL), listOf(matrix)),
                        ),
                        isRefreshing = false,
                        isOffline = true,
                    ),
                    onOpenSearch = {},
                    onFilterChange = {},
                    onOpenGenre = { openedGenre = it },
                    onOpenMedia = { openedMedia = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Нет подключения. Показаны сохранённые данные.").assertIsDisplayed()
        composeRule.onNodeWithText("В тренде за неделю").assertIsDisplayed()
        composeRule.onNodeWithText("Боевик").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Показать все: Боевик").performClick()
        assertEquals(action, openedGenre)

        composeRule.onNodeWithText("Во все тяжкие").performClick()
        assertEquals(MediaKey(MediaType.TV, 1396), openedMedia)
    }

    @Test
    fun homeShowsErrorWithRetry() {
        var retries = 0
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                HomeScreen(
                    uiState = HomeUiState(isRefreshing = false, error = DataError.NO_CONNECTION),
                    onOpenSearch = {},
                    onFilterChange = {},
                    onOpenGenre = {},
                    onOpenMedia = {},
                    onRetry = { retries++ },
                )
            }
        }

        composeRule.onNodeWithText("Нет подключения к интернету").assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun searchRowShowsYearTypeAndGenres() {
        var openedMedia: MediaKey? = null
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                val results = flowOf(PagingData.from(listOf(breakingBad))).collectAsLazyPagingItems()
                SearchScreen(
                    uiState = SearchUiState(
                        query = "тяжкие",
                        resultsQuery = "тяжкие",
                        genreNames = mapOf(MediaType.TV to mapOf(18 to "Драма", 80 to "Криминал")),
                    ),
                    results = results,
                    requestFocus = false,
                    onFocusRequested = {},
                    onQueryChange = {},
                    onFilterChange = {},
                    onOpenMedia = { openedMedia = it },
                )
            }
        }

        composeRule.onNodeWithText("2008 · Сериал").assertIsDisplayed()
        composeRule.onNodeWithText("Драма, Криминал").assertIsDisplayed()
        composeRule.onNodeWithText("Во все тяжкие").performClick()
        assertEquals(MediaKey(MediaType.TV, 1396), openedMedia)
    }

    @Test
    fun searchAsksForTwoCharacters() {
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                SearchScreen(
                    uiState = SearchUiState(query = "м"),
                    results = flowOf(PagingData.empty<Media>()).collectAsLazyPagingItems(),
                    requestFocus = true,
                    onFocusRequested = {},
                    onQueryChange = {},
                    onFilterChange = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithText("Введите хотя бы два символа названия").assertIsDisplayed()
    }

    @Test
    fun genreGridShowsPostersAndSortChips() {
        var sort: MediaSort? = null
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                GenreScreen(
                    uiState = GenreUiState(title = "Боевик", filter = MediaFilter.MOVIES),
                    media = flowOf(PagingData.from(listOf(matrix))).collectAsLazyPagingItems(),
                    onBack = {},
                    onSortChange = { sort = it },
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithText("Боевик: Фильмы").assertIsDisplayed()
        composeRule.onNodeWithText("Матрица").assertIsDisplayed()
        composeRule.onNodeWithText("По рейтингу").performClick()
        assertEquals(MediaSort.RATING, sort)
    }
}

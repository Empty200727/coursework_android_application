package ru.kinopolka.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.core.model.TmdbGenre
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.core.ui.theme.KinopolkaTheme
import ru.kinopolka.feature.about.AboutScreen
import ru.kinopolka.feature.details.DetailsScreen
import ru.kinopolka.feature.details.DetailsUiState
import ru.kinopolka.feature.genre.GenreScreen
import ru.kinopolka.feature.genre.GenreUiState
import ru.kinopolka.feature.home.HomeScreen
import ru.kinopolka.feature.home.HomeUiState
import ru.kinopolka.feature.home.ShelfContent
import ru.kinopolka.feature.library.LibraryScreen
import ru.kinopolka.feature.library.LibraryUiState
import ru.kinopolka.feature.library.PendingUndo
import ru.kinopolka.feature.search.SearchScreen
import ru.kinopolka.feature.search.SearchUiState

/** Renders the catalog screens with sample data: layout, texts and clicks (without a device). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "ru-w411dp-h891dp")
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

    private val details = MediaDetails(
        media = matrix.copy(
            key = MediaKey(MediaType.MOVIE, 550),
            title = "Бойцовский клуб",
            originalTitle = "Fight Club",
            overview = "An insomniac office worker forms an underground fight club.",
            releaseDate = LocalDate.of(1999, 10, 15),
            voteAverage = 8.4,
            voteCount = 29870,
        ),
        genres = listOf(TmdbGenre(MediaType.MOVIE, 18, "Драма")),
        runtimeMinutes = 139,
        numberOfSeasons = null,
        lastAirDate = null,
        inProduction = null,
        isOverviewFallback = true,
        cast = listOf(CastMember(819, "Эдвард Нортон", "Рассказчик", null, 0)),
        recommendations = emptyList(),
        similar = listOf(matrix),
    )

    @Test
    fun detailsShowsTheCardAndChangesTheLibrary() {
        var status: WatchStatus? = null
        var favoriteClicks = 0
        var openedMedia: MediaKey? = null
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                DetailsScreen(
                    uiState = DetailsUiState(key = details.media.key, details = details, isRefreshing = false),
                    onBack = {},
                    onOpenMedia = { openedMedia = it },
                    onStatusClick = { status = it },
                    onFavoriteClick = { favoriteClicks++ },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Fight Club").assertIsDisplayed()
        composeRule.onNodeWithText("1999 · Фильм · 2 ч 19 мин").assertIsDisplayed()
        composeRule.onNodeWithText("★ 8.4 · 29\u00a0870 оценок").assertIsDisplayed()
        composeRule.onNodeWithText("Описание на английском").assertIsDisplayed()

        composeRule.onNodeWithText("Хочу посмотреть").performClick()
        assertEquals(WatchStatus.WANT, status)
        composeRule.onNodeWithContentDescription("Добавить в избранное").performClick()
        assertEquals(1, favoriteClicks)

        // The outer lazy list of the card comes first in the tree; the cast row is scrollable too.
        val card = composeRule.onAllNodes(hasScrollToIndexAction()).onFirst()
        card.performScrollToNode(hasText("В ролях"))
        composeRule.onNodeWithText("Эдвард Нортон").assertIsDisplayed()
        card.performScrollToNode(hasText("Похожие"))
        composeRule.onNodeWithText("Матрица").performClick()
        assertEquals(matrix.key, openedMedia)
    }

    private val libraryItem = LibraryItem(
        entry = LibraryEntry(matrix.key, WatchStatus.WANT, false, Instant.EPOCH, null, null, null),
        media = matrix,
    )

    @Test
    fun libraryShowsTabsRemovesAndUndoes() {
        var removed: LibraryItem? = null
        var undone = false
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                LibraryScreen(
                    uiState = LibraryUiState(
                        items = listOf(libraryItem),
                        counts = mapOf(LibraryTab.WANT to 1, LibraryTab.WATCHED to 0, LibraryTab.FAVORITES to 0),
                        isLoading = false,
                        pendingUndo = PendingUndo(libraryItem.entry, "Матрица", LibraryTab.WANT),
                    ),
                    onOpenAbout = {},
                    onOpenMedia = {},
                    onFindSomething = {},
                    onTabChange = {},
                    onSortChange = {},
                    onFilterChange = {},
                    onRemove = { removed = it },
                    onUndo = { undone = true },
                    onUndoDismissed = {},
                )
            }
        }

        composeRule.onNodeWithText("Хочу (1)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Удалить из списка «Хочу»").performClick()
        assertEquals(libraryItem, removed)

        composeRule.onNodeWithText("«Матрица» удалено из списка «Хочу»").assertIsDisplayed()
        composeRule.onNodeWithText("Отменить").performClick()
        assertTrue(undone)
    }

    @Test
    fun emptyLibraryOffersToFindSomething() {
        var found = false
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                LibraryScreen(
                    uiState = LibraryUiState(isLoading = false),
                    onOpenAbout = {},
                    onOpenMedia = {},
                    onFindSomething = { found = true },
                    onTabChange = {},
                    onSortChange = {},
                    onFilterChange = {},
                    onRemove = {},
                    onUndo = {},
                    onUndoDismissed = {},
                )
            }
        }

        composeRule.onNodeWithText("Найти, что посмотреть").performClick()
        assertTrue(found)
    }

    @Test
    fun aboutShowsVersionAndTmdbAttribution() {
        composeRule.setContent {
            KinopolkaTheme(dynamicColor = false) {
                AboutScreen(onBack = {}, versionName = "1.0.0")
            }
        }

        composeRule.onNodeWithText("Версия 1.0.0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Логотип TMDB").assertIsDisplayed()
        composeRule.onNodeWithText("This product uses the TMDB API but is not endorsed or certified by TMDB.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("themoviedb.org").assertExists()
    }
}

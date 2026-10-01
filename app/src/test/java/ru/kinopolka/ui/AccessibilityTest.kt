package ru.kinopolka.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
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
import ru.kinopolka.feature.search.SearchScreen
import ru.kinopolka.feature.search.SearchUiState

/**
 * N-08: on every screen each clickable element has a touch area of at least 48 dp and a label
 * for TalkBack, also with the font scaled to 150%.
 *
 * The Accessibility Test Framework of Compose does not report problems under Robolectric (it
 * passes even a 12 dp unlabeled button), so these rules are checked here directly; contrast is
 * checked on a device with Accessibility Scanner.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "ru-w411dp-h891dp")
class AccessibilityTest {

    @Suppress("DEPRECATION") // v1 rule: Paging delivers items under Robolectric (see ScreensSmokeTest).
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val action = Genre(key = "action", name = "Боевик", movieGenreId = 28, tvGenreId = 10759)
    private val media = Media(
        key = MediaKey(MediaType.TV, 1396),
        title = "Во все тяжкие",
        originalTitle = "Breaking Bad",
        overview = "Школьный учитель химии Уолтер Уайт узнаёт, что болен раком лёгких.",
        posterPath = null,
        backdropPath = null,
        releaseDate = LocalDate.of(2008, 1, 20),
        voteAverage = 8.9,
        voteCount = 15432,
        popularity = 312.5,
        genreIds = listOf(18),
    )
    private val details = MediaDetails(
        media = media,
        genres = emptyList(),
        runtimeMinutes = null,
        numberOfSeasons = 5,
        lastAirDate = LocalDate.of(2013, 9, 29),
        inProduction = false,
        isOverviewFallback = false,
        cast = listOf(CastMember(17419, "Брайан Крэнстон", "Уолтер Уайт", null, 0)),
        recommendations = listOf(media.copy(key = MediaKey(MediaType.TV, 60059), title = "Лучше звоните Солу")),
        similar = emptyList(),
    )

    private fun check(fontScale: Float, content: @Composable () -> Unit) {
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent { KinopolkaTheme(dynamicColor = false) { content() } }
        val problems = accessibilityProblems()
        assertTrue("Accessibility problems:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    /** Clickable elements with a touch area under 48 dp or without text or description. */
    private fun accessibilityProblems(): List<String> {
        val minSize = with(composeRule.density) { 48.dp.toPx() } - 1f
        val clickable = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        // Elements composed outside the screen (e.g. the next card of a list) have empty bounds.
        return clickable.filter { !it.boundsInRoot.isEmpty }.mapNotNull { node ->
            val bounds = node.touchBoundsInRoot
            val label = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
                listOfNotNull(node.config.getOrNull(SemanticsProperties.EditableText)?.text)
            when {
                bounds.width < minSize || bounds.height < minSize -> "touch area ${bounds.size} of $label"
                label.none { it.isNotBlank() } -> "no label: ${node.config}"
                else -> null
            }
        }
    }

    private fun homeContent() = @Composable {
        HomeScreen(
            uiState = HomeUiState(
                shelves = listOf(
                    ShelfContent(Shelf.Trending(MediaFilter.ALL), listOf(media)),
                    ShelfContent(Shelf.ByGenre(action, MediaFilter.ALL), listOf(media)),
                ),
                isRefreshing = false,
                isOffline = true,
            ),
            onOpenSearch = {},
            onFilterChange = {},
            onOpenGenre = {},
            onOpenMedia = {},
            onRetry = {},
        )
    }

    @Test
    fun home() = check(1f, homeContent())

    @Test
    fun homeLargeFont() = check(LARGE_FONT, homeContent())

    private fun searchContent() = @Composable {
        SearchScreen(
            uiState = SearchUiState(query = "тяжкие", resultsQuery = "тяжкие"),
            results = flowOf(PagingData.from(listOf(media))).collectAsLazyPagingItems(),
            requestFocus = false,
            onFocusRequested = {},
            onQueryChange = {},
            onFilterChange = {},
            onOpenMedia = {},
        )
    }

    @Test
    fun search() = check(1f, searchContent())

    @Test
    fun searchLargeFont() = check(LARGE_FONT, searchContent())

    @Test
    fun genre() = check(LARGE_FONT) {
        GenreScreen(
            uiState = GenreUiState(title = "Боевик"),
            media = flowOf(PagingData.from(listOf(media))).collectAsLazyPagingItems(),
            onBack = {},
            onSortChange = {},
            onOpenMedia = {},
        )
    }

    private fun card() = @Composable {
        DetailsScreen(
            uiState = DetailsUiState(key = media.key, details = details, isRefreshing = false),
            onBack = {},
            onOpenMedia = {},
            onStatusClick = {},
            onFavoriteClick = {},
            onRetry = {},
        )
    }

    @Test
    fun details() = check(1f, card())

    @Test
    fun detailsLargeFont() = check(LARGE_FONT, card())

    @Test
    fun library() = check(LARGE_FONT) {
        LibraryScreen(
            uiState = LibraryUiState(
                items = listOf(
                    LibraryItem(
                        LibraryEntry(media.key, WatchStatus.WANT, true, Instant.EPOCH, null, null, null),
                        media,
                    ),
                ),
                counts = mapOf(LibraryTab.WANT to 1, LibraryTab.WATCHED to 0, LibraryTab.FAVORITES to 1),
                isLoading = false,
            ),
            onOpenAbout = {},
            onOpenMedia = {},
            onFindSomething = {},
            onTabChange = {},
            onSortChange = {},
            onFilterChange = {},
            onRemove = {},
            onUndo = {},
            onUndoDismissed = {},
        )
    }

    /** The checks do find problems: a tiny unlabeled button is reported twice. */
    @Test
    fun checksDetectProblems() {
        composeRule.setContent {
            Row {
                Box(Modifier.size(12.dp).clickable {})
                Box(Modifier.size(56.dp).clickable {})
            }
        }
        val problems = accessibilityProblems()
        assertEquals(problems.toString(), 2, problems.size)
    }

    @Test
    fun about() = check(LARGE_FONT) { AboutScreen(onBack = {}, versionName = "1.0.0") }

    private companion object {
        const val LARGE_FONT = 1.5f
    }
}

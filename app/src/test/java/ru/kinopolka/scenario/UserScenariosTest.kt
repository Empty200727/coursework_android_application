package ru.kinopolka.scenario

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.PagingData
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.kinopolka.MainActivity
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.FakeDetailsRepository
import ru.kinopolka.testing.FakeLibraryRepository
import ru.kinopolka.testing.FakeMediaRepository
import ru.kinopolka.testing.FakeNetworkMonitor

/**
 * End-to-end scenarios of docs/PLAN.md, section 8, on the real activity and navigation with fake
 * repositories (Hilt test module): search → card → «Хочу посмотреть»; «Моя полка» without
 * network; removal with «Отменить».
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "ru-w411dp-h891dp")
class UserScenariosTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Suppress("DEPRECATION") // v1 rule: Paging delivers items under Robolectric (see ScreensSmokeTest).
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var mediaRepository: FakeMediaRepository

    @Inject lateinit var detailsRepository: FakeDetailsRepository

    @Inject lateinit var libraryRepository: FakeLibraryRepository

    @Inject lateinit var networkMonitor: FakeNetworkMonitor

    private val matrix = Media(
        key = MediaKey(MediaType.MOVIE, 603),
        title = "Матрица",
        originalTitle = "The Matrix",
        overview = "Жизнь Томаса Андерсона разделена на две части.",
        posterPath = null,
        backdropPath = null,
        releaseDate = LocalDate.of(1999, 3, 31),
        voteAverage = 8.2,
        voteCount = 25643,
        popularity = 85.3,
        genreIds = listOf(28),
    )

    private val matrixDetails = MediaDetails(
        media = matrix,
        genres = emptyList(),
        runtimeMinutes = 136,
        numberOfSeasons = null,
        lastAirDate = null,
        inProduction = null,
        isOverviewFallback = false,
        cast = emptyList(),
        recommendations = emptyList(),
        similar = emptyList(),
    )

    @Before
    fun setUp() {
        hiltRule.inject()
        mediaRepository.searchResults = { _, _ -> flowOf(PagingData.from(listOf(matrix))) }
        detailsRepository.details.value = mapOf(matrix.key to matrixDetails)
        libraryRepository.media.value = mapOf(matrix.key to matrix)
    }

    /** Lets the 400 ms search debounce on the main looper pass. */
    private fun waitForDebounce() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SEARCH_WAIT_MILLIS))
        composeRule.waitForIdle()
    }

    @Test
    fun searchOpenCardAndAddToWantList() {
        composeRule.onNodeWithText("Поиск фильмов и сериалов").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("матрица")
        waitForDebounce()

        composeRule.onNodeWithText("1999 · Фильм").performClick()
        composeRule.onNodeWithText("The Matrix").assertIsDisplayed()
        composeRule.onNodeWithText("Хочу посмотреть").performClick()

        assertEquals(WatchStatus.WANT, libraryRepository.entries.value[matrix.key]?.watchStatus)
        assertEquals(listOf("матрица"), mediaRepository.searches.map { it.first })

        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithText("Моя полка").performClick()
        composeRule.onNodeWithText("Хочу (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Матрица").assertIsDisplayed()
    }

    @Test
    fun libraryAndSavedCardWorkOffline() {
        networkMonitor.online.value = false
        libraryRepository.entries.value = mapOf(
            matrix.key to LibraryEntry(matrix.key, WatchStatus.WATCHED, true, Instant.EPOCH, Instant.EPOCH, null, null),
        )

        composeRule.onNodeWithText("Моя полка").performClick()
        composeRule.onNodeWithText("Смотрел (1)").performClick()
        composeRule.onNodeWithText("Матрица").performClick()

        composeRule.onNodeWithText("Нет подключения. Показаны сохранённые данные.").assertIsDisplayed()
        composeRule.onNodeWithText("The Matrix").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Убрать из избранного").assertIsDisplayed()
        assertEquals("no network requests offline", emptyList<MediaKey>(), detailsRepository.refreshed)
    }

    @Test
    fun removeFromLibraryAndUndo() {
        libraryRepository.entries.value = mapOf(
            matrix.key to LibraryEntry(matrix.key, WatchStatus.WANT, false, Instant.EPOCH, null, null, null),
        )
        composeRule.onNodeWithText("Моя полка").performClick()

        composeRule.onNodeWithContentDescription("Удалить из списка «Хочу»").performClick()
        composeRule.onNodeWithText("«Матрица» удалено из списка «Хочу»").assertIsDisplayed()
        composeRule.onNodeWithText("Хочу (0)").assertIsDisplayed()

        composeRule.onNodeWithText("Отменить").performClick()

        composeRule.onNodeWithText("Хочу (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Матрица").assertIsDisplayed()
        assertEquals(WatchStatus.WANT, libraryRepository.entries.value[matrix.key]?.watchStatus)
    }

    private companion object {
        const val SEARCH_WAIT_MILLIS = 500L
    }
}

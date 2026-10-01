package ru.kinopolka.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.FakeDetailsRepository
import ru.kinopolka.testing.FakeLibraryRepository
import ru.kinopolka.testing.FakeNetworkMonitor
import ru.kinopolka.testing.MainDispatcherRule
import ru.kinopolka.testing.testMedia

/** F-11: the card changes the library; offline it shows the cache. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val key = MediaKey(MediaType.MOVIE, 550)
    private val detailsRepository = FakeDetailsRepository()
    private val libraryRepository = FakeLibraryRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private val details = MediaDetails(
        media = testMedia(550, releaseDate = LocalDate.of(1999, 10, 15)),
        genres = emptyList(),
        runtimeMinutes = 139,
        numberOfSeasons = null,
        lastAirDate = null,
        inProduction = null,
        isOverviewFallback = false,
        cast = emptyList(),
        recommendations = emptyList(),
        similar = emptyList(),
    )

    private fun TestScope.createViewModel(): DetailsViewModel {
        val savedState = SavedStateHandle(mapOf("mediaTypeKey" to "movie", "id" to 550))
        val viewModel = DetailsViewModel(savedState, detailsRepository, libraryRepository, networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `route arguments select the title and the card is refreshed`() = runTest {
        detailsRepository.details.value = mapOf(key to details)
        val viewModel = createViewModel()

        assertEquals(key, viewModel.uiState.value.key)
        assertEquals(DetailsContent.DATA, viewModel.uiState.value.content)
        assertEquals(listOf(key), detailsRepository.refreshed)
    }

    @Test
    fun `want and watched exclude each other and a second tap clears the status`() = runTest {
        detailsRepository.details.value = mapOf(key to details)
        val viewModel = createViewModel()

        viewModel.onStatusClick(WatchStatus.WANT)
        assertEquals(WatchStatus.WANT, viewModel.uiState.value.watchStatus)

        viewModel.onStatusClick(WatchStatus.WATCHED)
        assertEquals(WatchStatus.WATCHED, viewModel.uiState.value.watchStatus)

        viewModel.onStatusClick(WatchStatus.WATCHED)
        assertNull("no status and no favorite: entry removed", viewModel.uiState.value.entry)
    }

    @Test
    fun `favorite is toggled independently`() = runTest {
        detailsRepository.details.value = mapOf(key to details)
        val viewModel = createViewModel()
        viewModel.onStatusClick(WatchStatus.WATCHED)

        viewModel.onFavoriteClick()
        assertTrue(viewModel.uiState.value.isFavorite)
        assertEquals(WatchStatus.WATCHED, viewModel.uiState.value.watchStatus)

        viewModel.onFavoriteClick()
        assertFalse(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `offline the cached card is shown without a request`() = runTest {
        networkMonitor.online.value = false
        detailsRepository.details.value = mapOf(key to details)
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isOffline)
        assertEquals(DetailsContent.DATA, state.content)
        assertTrue(detailsRepository.refreshed.isEmpty())
    }

    @Test
    fun `nothing cached and loading failed shows the error and retry works`() = runTest {
        detailsRepository.refreshResult = RefreshResult.Failed(DataError.SERVER)
        val viewModel = createViewModel()
        assertEquals(DetailsContent.ERROR, viewModel.uiState.value.content)

        detailsRepository.refreshResult = RefreshResult.Updated
        detailsRepository.details.value = mapOf(key to details)
        viewModel.retry()

        assertEquals(DetailsContent.DATA, viewModel.uiState.value.content)
        assertEquals(2, detailsRepository.refreshed.size)
    }

    @Test
    fun `years and runtime are formatted`() {
        assertEquals("1999", details.yearsText(ongoing = "н. в."))
        val series = details.copy(
            media = testMedia(1, MediaType.TV, releaseDate = LocalDate.of(2008, 1, 20)),
            lastAirDate = LocalDate.of(2013, 9, 29),
            inProduction = false,
        )
        assertEquals("2008–2013", series.yearsText(ongoing = "н. в."))
        assertEquals("2008–н. в.", series.copy(inProduction = true).yearsText(ongoing = "н. в."))
        assertEquals(2 to 19, splitRuntime(139))
    }
}

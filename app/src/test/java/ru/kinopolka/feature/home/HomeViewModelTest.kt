package ru.kinopolka.feature.home

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.testing.FakeGenreRepository
import ru.kinopolka.testing.FakeMediaRepository
import ru.kinopolka.testing.FakeNetworkMonitor
import ru.kinopolka.testing.MainDispatcherRule
import ru.kinopolka.testing.testMedia

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val comedy = Genre(key = "comedy", name = "Комедия", movieGenreId = 35, tvGenreId = 35)
    private val horror = Genre(key = "horror", name = "Ужасы", movieGenreId = 27, tvGenreId = null)

    private val genreRepository = FakeGenreRepository(listOf(comedy, horror))
    private val mediaRepository = FakeMediaRepository()
    private val networkMonitor = FakeNetworkMonitor()
    private val savedState = SavedStateHandle()

    private fun TestScope.createViewModel(): HomeViewModel {
        val viewModel = HomeViewModel(savedState, mediaRepository, genreRepository, networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun fillShelves() {
        mediaRepository.onRefresh = { shelf ->
            mediaRepository.shelves.value = mediaRepository.shelves.value + (shelf to listOf(testMedia(1)))
        }
    }

    @Test
    fun `shows loading until shelves are refreshed`() = runTest {
        genreRepository.nextResult = CompletableDeferred()
        fillShelves()
        val viewModel = createViewModel()

        assertEquals(HomeContent.LOADING, viewModel.uiState.value.content)

        genreRepository.nextResult.complete(RefreshResult.Skipped)

        val state = viewModel.uiState.value
        assertEquals(HomeContent.DATA, state.content)
        assertFalse(state.isRefreshing)
        assertEquals(
            listOf(
                Shelf.Trending(MediaFilter.ALL),
                Shelf.Popular(MediaType.MOVIE),
                Shelf.Popular(MediaType.TV),
                Shelf.ByGenre(comedy, MediaFilter.ALL),
                Shelf.ByGenre(horror, MediaFilter.ALL),
            ),
            state.shelves.map { it.shelf },
        )
    }

    @Test
    fun `every shelf is refreshed`() = runTest {
        createViewModel()

        assertEquals(5, mediaRepository.refreshedShelves.size)
        assertEquals(1, genreRepository.refreshCalls)
    }

    @Test
    fun `filter changes the shelves and is saved`() = runTest {
        fillShelves()
        val viewModel = createViewModel()

        viewModel.onFilterChange(MediaFilter.SERIES)

        val state = viewModel.uiState.value
        assertEquals(MediaFilter.SERIES, state.filter)
        assertEquals(
            listOf(
                Shelf.Trending(MediaFilter.SERIES),
                Shelf.Popular(MediaType.TV),
                Shelf.ByGenre(comedy, MediaFilter.SERIES),
            ),
            state.shelves.map { it.shelf },
        )
        assertEquals(MediaFilter.SERIES.name, savedState.get<String>("filter"))
    }

    @Test
    fun `nothing cached and refresh failed shows the error`() = runTest {
        mediaRepository.refreshResult = RefreshResult.Failed(DataError.SERVER)
        val viewModel = createViewModel()

        assertEquals(HomeContent.ERROR, viewModel.uiState.value.content)
        assertEquals(DataError.SERVER, viewModel.uiState.value.error)
    }

    @Test
    fun `offline shows the cache with the banner and refreshes when the connection returns`() = runTest {
        networkMonitor.online.value = false
        mediaRepository.shelves.value = mapOf(Shelf.Trending(MediaFilter.ALL) to listOf(testMedia(7)))
        val viewModel = createViewModel()

        val offline = viewModel.uiState.value
        assertTrue(offline.isOffline)
        assertEquals(HomeContent.DATA, offline.content)
        assertTrue("no requests without network", mediaRepository.refreshedShelves.isEmpty())

        networkMonitor.online.value = true

        assertFalse(viewModel.uiState.value.isOffline)
        assertEquals(5, mediaRepository.refreshedShelves.size)
    }

    @Test
    fun `retry refreshes again`() = runTest {
        mediaRepository.refreshResult = RefreshResult.Failed(DataError.NO_CONNECTION)
        val viewModel = createViewModel()
        assertEquals(HomeContent.ERROR, viewModel.uiState.value.content)

        mediaRepository.refreshResult = RefreshResult.Updated
        fillShelves()
        viewModel.retry()

        assertEquals(HomeContent.DATA, viewModel.uiState.value.content)
        assertEquals(10, mediaRepository.refreshedShelves.size)
    }
}

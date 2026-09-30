package ru.kinopolka.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.testing.FakeGenreRepository
import ru.kinopolka.testing.FakeMediaRepository
import ru.kinopolka.testing.FakeNetworkMonitor
import ru.kinopolka.testing.MainDispatcherRule

/** F-01, F-02, N-05: debounce, minimal length, cancellation, filter and saved state. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mediaRepository = FakeMediaRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun TestScope.createViewModel(savedState: SavedStateHandle = SavedStateHandle()): SearchViewModel {
        val viewModel = SearchViewModel(savedState, mediaRepository, FakeGenreRepository(), networkMonitor)
        backgroundScope.launch { viewModel.results.collect {} }
        return viewModel
    }

    @Test
    fun `request goes out 400 ms after typing stops`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("ма")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertTrue(mediaRepository.searches.isEmpty())

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf("ма" to MediaFilter.ALL), mediaRepository.searches)
    }

    @Test
    fun `only the last query of fast typing is searched`() = runTest {
        val viewModel = createViewModel()

        listOf("м", "ма", "мат", "матрица").forEach {
            viewModel.onQueryChange(it)
            advanceTimeBy(100)
        }
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(listOf("матрица" to MediaFilter.ALL), mediaRepository.searches)
    }

    @Test
    fun `queries shorter than two characters are not sent`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("м")
        advanceTimeBy(1_000)
        viewModel.onQueryChange("  д  ")
        advanceTimeBy(1_000)
        runCurrent()

        assertTrue(mediaRepository.searches.isEmpty())
        assertTrue(viewModel.uiState.value.isQueryTooShort)
    }

    @Test
    fun `query is trimmed and a repeated query is not sent twice`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("дюна ")
        advanceTimeBy(500)
        viewModel.onQueryChange("дюна")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(listOf("дюна" to MediaFilter.ALL), mediaRepository.searches)
    }

    @Test
    fun `new query cancels the previous request`() = runTest {
        val cancelled = mutableListOf<String>()
        mediaRepository.searchResults = { query, _ ->
            flow<PagingData<Media>> {
                emit(PagingData.empty())
                awaitCancellation()
            }.onCompletion { cancelled += query }
        }
        val viewModel = createViewModel()

        viewModel.onQueryChange("дюна")
        advanceTimeBy(500)
        viewModel.onQueryChange("матрица")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(listOf("дюна", "матрица"), mediaRepository.searches.map { it.first })
        assertEquals(listOf("дюна"), cancelled)
    }

    @Test
    fun `filter change searches again without waiting`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("тяжкие")
        advanceTimeBy(500)

        viewModel.onFilterChange(MediaFilter.SERIES)
        runCurrent()

        assertEquals(
            listOf("тяжкие" to MediaFilter.ALL, "тяжкие" to MediaFilter.SERIES),
            mediaRepository.searches,
        )
    }

    @Test
    fun `query and filter are restored from the saved state`() = runTest {
        val savedState = SavedStateHandle(mapOf("query" to "дюна", "filter" to MediaFilter.MOVIES.name))
        val viewModel = createViewModel(savedState)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("дюна", state.query)
            assertEquals(MediaFilter.MOVIES, state.filter)
        }
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf("дюна" to MediaFilter.MOVIES), mediaRepository.searches)
    }

    @Test
    fun `changes are written to the saved state`() = runTest {
        val savedState = SavedStateHandle()
        val viewModel = createViewModel(savedState)

        viewModel.onQueryChange("сол")
        viewModel.onFilterChange(MediaFilter.SERIES)

        assertEquals("сол", savedState.get<String>("query"))
        assertEquals(MediaFilter.SERIES.name, savedState.get<String>("filter"))
    }

    @Test
    fun `offline state is shown`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().isOffline)
            networkMonitor.online.value = false
            assertTrue(awaitItem().isOffline)
        }
    }

    @Test
    fun `results are pending while the debounce runs`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onQueryChange("дюна")
        runCurrent()
        assertTrue(viewModel.uiState.value.isPending)

        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()
        assertFalse(viewModel.uiState.value.isPending)
    }
}

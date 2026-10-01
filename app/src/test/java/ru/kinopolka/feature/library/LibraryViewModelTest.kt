package ru.kinopolka.feature.library

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.kinopolka.core.model.LibrarySort
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.FakeLibraryRepository
import ru.kinopolka.testing.MainDispatcherRule
import ru.kinopolka.testing.testMedia

/** F-12, F-13: tabs, counters, filter and removal with «Отменить». */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    private val savedState = SavedStateHandle()
    private val matrix = testMedia(603).copy(title = "Матрица")
    private val arcane = testMedia(94605, MediaType.TV).copy(title = "Аркейн")

    private suspend fun fill() {
        repository.media.value = mapOf(matrix.key to matrix, arcane.key to arcane)
        repository.setWatchStatus(matrix.key, WatchStatus.WANT)
        repository.setWatchStatus(arcane.key, WatchStatus.WANT)
        repository.setFavorite(arcane.key, true)
    }

    private fun TestScope.createViewModel(): LibraryViewModel {
        val viewModel = LibraryViewModel(savedState, repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun LibraryViewModel.ids() = uiState.value.items.map { it.media.key }

    @Test
    fun `tabs show their titles with counters`() = runTest {
        fill()
        val viewModel = createViewModel()

        assertEquals(LibraryTab.WANT, viewModel.uiState.value.tab)
        assertEquals(
            mapOf(LibraryTab.WANT to 2, LibraryTab.WATCHED to 0, LibraryTab.FAVORITES to 1),
            viewModel.uiState.value.counts,
        )

        viewModel.onTabChange(LibraryTab.FAVORITES)
        assertEquals(listOf(arcane.key), viewModel.ids())
        assertEquals(LibraryTab.FAVORITES.name, savedState.get<String>("tab"))
    }

    @Test
    fun `sort and type filter are applied and saved`() = runTest {
        fill()
        val viewModel = createViewModel()

        viewModel.onSortChange(LibrarySort.TITLE)
        assertEquals(listOf(arcane.key, matrix.key), viewModel.ids())

        viewModel.onFilterChange(MediaFilter.MOVIES)
        assertEquals(listOf(matrix.key), viewModel.ids())
        assertEquals(1, viewModel.uiState.value.counts[LibraryTab.WANT])
        assertEquals(LibrarySort.TITLE.name, savedState.get<String>("sort"))
        assertEquals(MediaFilter.MOVIES.name, savedState.get<String>("filter"))
    }

    @Test
    fun `removal can be undone`() = runTest {
        fill()
        val viewModel = createViewModel()
        val item = viewModel.uiState.value.items.single { it.media.key == matrix.key }

        viewModel.onRemove(item)
        assertEquals(listOf(arcane.key), viewModel.ids())
        assertEquals("Матрица", viewModel.uiState.value.pendingUndo?.title)

        viewModel.onUndo()
        assertNull(viewModel.uiState.value.pendingUndo)
        assertEquals(setOf(matrix.key, arcane.key), viewModel.ids().toSet())
        assertTrue(repository.releasedPosters.isEmpty())
    }

    @Test
    fun `removal becomes final when the snackbar is dismissed`() = runTest {
        fill()
        val viewModel = createViewModel()
        val item = viewModel.uiState.value.items.single { it.media.key == matrix.key }

        viewModel.onRemove(item)
        viewModel.onUndoDismissed()

        assertNull(repository.entries.value[matrix.key])
        assertEquals(listOf(matrix.key), repository.releasedPosters.map { it.key })
    }

    @Test
    fun `removal from favorites keeps the status`() = runTest {
        fill()
        val viewModel = createViewModel()
        viewModel.onTabChange(LibraryTab.FAVORITES)

        viewModel.onRemove(viewModel.uiState.value.items.single())

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(WatchStatus.WANT, repository.entries.value[MediaKey(MediaType.TV, 94605)]?.watchStatus)
    }
}

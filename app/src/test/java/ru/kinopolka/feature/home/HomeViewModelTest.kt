package ru.kinopolka.feature.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.model.Genre
import ru.kinopolka.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val comedy = Genre(key = "comedy", name = "Комедия", movieGenreId = 35, tvGenreId = 35)

    private class FakeGenreRepository : GenreRepository {
        val genres = MutableStateFlow<List<Genre>>(emptyList())
        var nextResult = CompletableDeferred<RefreshResult>()
        var refreshCalls = 0

        override fun observeGenres(): Flow<List<Genre>> = genres

        override suspend fun getGenre(key: String): Genre? = genres.value.firstOrNull { it.key == key }

        override suspend fun refresh(force: Boolean): RefreshResult {
            refreshCalls++
            return nextResult.await()
        }
    }

    private val repository = FakeGenreRepository()

    private fun TestScope.createViewModel(): HomeViewModel {
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `shows loading and then the genres`() = runTest {
        val viewModel = createViewModel()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        repository.genres.value = listOf(comedy)
        repository.nextResult.complete(RefreshResult.Updated)

        assertEquals(HomeUiState.Success(listOf(comedy)), viewModel.uiState.value)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `shows an error when nothing is cached and loading failed`() = runTest {
        val viewModel = createViewModel()

        repository.nextResult.complete(RefreshResult.Failed(DataError.NO_CONNECTION))

        assertEquals(HomeUiState.Error(DataError.NO_CONNECTION), viewModel.uiState.value)
    }

    @Test
    fun `keeps cached genres when the update failed`() = runTest {
        repository.genres.value = listOf(comedy)
        val viewModel = createViewModel()
        assertEquals(HomeUiState.Success(listOf(comedy)), viewModel.uiState.value)

        repository.nextResult.complete(RefreshResult.Failed(DataError.UNAUTHORIZED))

        assertEquals(
            HomeUiState.Success(listOf(comedy), refreshError = DataError.UNAUTHORIZED),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `retry refreshes again`() = runTest {
        repository.nextResult.complete(RefreshResult.Failed(DataError.NO_CONNECTION))
        val viewModel = createViewModel()
        assertEquals(HomeUiState.Error(DataError.NO_CONNECTION), viewModel.uiState.value)

        repository.nextResult = CompletableDeferred()
        viewModel.retry()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        repository.genres.value = listOf(comedy)
        repository.nextResult.complete(RefreshResult.Updated)
        assertEquals(HomeUiState.Success(listOf(comedy)), viewModel.uiState.value)
        assertEquals(2, repository.refreshCalls)
    }
}

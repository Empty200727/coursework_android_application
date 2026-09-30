package ru.kinopolka.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.util.NetworkMonitor
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.Shelf

/**
 * Home screen (F-02, F-05, F-07): shelves are read from Room, stale ones are refreshed in the
 * background, and everything is refreshed again when the connection comes back (N-02).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val genreRepository: GenreRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private data class RefreshState(val isRefreshing: Boolean = true, val error: DataError? = null)

    private val filter: StateFlow<MediaFilter> = savedStateHandle.getStateFlow(FILTER_KEY, MediaFilter.ALL.name)
        .map { name -> MediaFilter.entries.firstOrNull { it.name == name } ?: MediaFilter.ALL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MediaFilter.ALL)

    private val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val shelves: Flow<List<Shelf>> = combine(filter, genreRepository.observeGenres()) { filter, genres ->
        homeShelves(filter, genres)
    }.distinctUntilChanged()

    private val refreshState = MutableStateFlow(RefreshState())
    private val retryRequests = MutableStateFlow(0)

    private val shelfContents: Flow<List<ShelfContent>> = shelves.flatMapLatest { shelves ->
        combine(shelves.map { shelf -> mediaRepository.observeShelf(shelf).map { ShelfContent(shelf, it) } }) {
            it.toList()
        }
    }.onStart { emit(emptyList()) }

    val uiState: StateFlow<HomeUiState> = combine(filter, shelfContents, refreshState, isOnline) {
            filter,
            contents,
            refresh,
            online,
        ->
        HomeUiState(
            filter = filter,
            shelves = contents.filter { it.shelf.filterMatches(filter) },
            isRefreshing = refresh.isRefreshing,
            error = refresh.error,
            isOffline = !online,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            combine(shelves, isOnline, retryRequests) { shelves, online, _ -> shelves to online }
                .collectLatest { (shelves, online) -> refresh(shelves, online) }
        }
    }

    fun onFilterChange(filter: MediaFilter) {
        savedStateHandle[FILTER_KEY] = filter.name
    }

    fun retry() {
        retryRequests.value++
    }

    private suspend fun refresh(shelves: List<Shelf>, online: Boolean) {
        if (!online) {
            refreshState.value = RefreshState(isRefreshing = false, error = DataError.NO_CONNECTION)
            return
        }
        refreshState.value = RefreshState(isRefreshing = true)
        val genres = genreRepository.refresh()
        val results = coroutineScope {
            shelves.map { shelf -> async { mediaRepository.refreshShelf(shelf) } }.awaitAll()
        }
        val error = (listOf(genres) + results).firstNotNullOfOrNull { (it as? RefreshResult.Failed)?.error }
        refreshState.value = RefreshState(isRefreshing = false, error = error)
    }

    // A shelf list built for the previous filter may still be on screen for one frame.
    private fun Shelf.filterMatches(filter: MediaFilter): Boolean = when (this) {
        is Shelf.Trending -> this.filter == filter
        is Shelf.Popular -> filter.includes(mediaType)
        is Shelf.ByGenre -> this.filter == filter
    }

    private companion object {
        const val FILTER_KEY = "filter"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

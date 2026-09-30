package ru.kinopolka.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.util.NetworkMonitor
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    /** Genre names from the local catalog for result rows. */
    val genreNames: Map<MediaType, Map<Int, String>> = emptyMap(),
    val isOffline: Boolean = false,
    /** Query of the results on screen; differs from [query] while the debounce is running. */
    val resultsQuery: String? = null,
) {
    /** F-01: a request is sent only from [SEARCH_MIN_QUERY_LENGTH] characters. */
    val isQueryTooShort: Boolean get() = query.trim().length < SEARCH_MIN_QUERY_LENGTH

    /** The query was changed and its results are not requested yet. */
    val isPending: Boolean get() = !isQueryTooShort && query.trim() != resultsQuery
}

const val SEARCH_MIN_QUERY_LENGTH = 2
const val SEARCH_DEBOUNCE_MILLIS = 400L

/**
 * Search (F-01…F-03). The query and the filter live in [SavedStateHandle] (N-05). A request
 * goes out 400 ms after typing stops, from 2 characters; a new query cancels the previous one.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val genreRepository: GenreRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

    private val filter: Flow<MediaFilter> = savedStateHandle.getStateFlow(FILTER_KEY, MediaFilter.ALL.name)
        .map { name -> MediaFilter.entries.firstOrNull { it.name == name } ?: MediaFilter.ALL }

    private val resultsQuery = MutableStateFlow<String?>(null)

    /** Only the query is debounced: switching the filter searches again at once. */
    val results: Flow<PagingData<Media>> = combine(
        query.debounce(SEARCH_DEBOUNCE_MILLIS).map { it.trim() },
        filter,
    ) { query, filter -> query to filter }
        .distinctUntilChanged()
        .flatMapLatest { (query, filter) ->
            resultsQuery.value = query
            if (query.length < SEARCH_MIN_QUERY_LENGTH) {
                flowOf(PagingData.empty())
            } else {
                mediaRepository.search(query, filter)
            }
        }
        .cachedIn(viewModelScope)

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        filter,
        genreRepository.observeGenreNames(),
        networkMonitor.isOnline,
        resultsQuery,
    ) { query, filter, genreNames, online, resultsQuery ->
        SearchUiState(
            query = query,
            filter = filter,
            genreNames = genreNames,
            isOffline = !online,
            resultsQuery = resultsQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SearchUiState(
            query = savedStateHandle[QUERY_KEY] ?: "",
            filter = MediaFilter.entries.firstOrNull { it.name == savedStateHandle[FILTER_KEY] } ?: MediaFilter.ALL,
        ),
    )

    init {
        // Result rows show genre names from the catalog: make sure it is loaded.
        viewModelScope.launch { genreRepository.refresh() }
    }

    fun onQueryChange(query: String) {
        savedStateHandle[QUERY_KEY] = query
    }

    fun onFilterChange(filter: MediaFilter) {
        savedStateHandle[FILTER_KEY] = filter.name
    }

    private companion object {
        const val QUERY_KEY = "query"
        const val FILTER_KEY = "filter"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

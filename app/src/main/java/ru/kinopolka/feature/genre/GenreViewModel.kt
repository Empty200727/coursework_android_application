package ru.kinopolka.feature.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.util.NetworkMonitor
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.navigation.GenreRoute

data class GenreUiState(
    /** `null` until the genre is found in the catalog. */
    val title: String? = null,
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: MediaSort = MediaSort.POPULARITY,
    val isOffline: Boolean = false,
)

/** Genre screen (F-06): paged grid with sorting; the sort order lives in [SavedStateHandle]. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GenreViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    mediaRepository: MediaRepository,
    private val genreRepository: GenreRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val route: GenreRoute = savedStateHandle.toRoute()

    private val genre: Flow<Genre?> = genreRepository.observeGenres()
        .map { genres -> genres.firstOrNull { it.key == route.genreKey } }
        .distinctUntilChanged()

    private val sort: Flow<MediaSort> = savedStateHandle.getStateFlow(SORT_KEY, MediaSort.POPULARITY.name)
        .map { name -> MediaSort.entries.firstOrNull { it.name == name } ?: MediaSort.POPULARITY }

    val media: Flow<PagingData<Media>> = combine(genre, sort) { genre, sort -> genre to sort }
        .distinctUntilChanged()
        .flatMapLatest { (genre, sort) ->
            if (genre == null) emptyFlow() else mediaRepository.genreMedia(genre, route.mediaFilter, sort)
        }
        .cachedIn(viewModelScope)

    val uiState: StateFlow<GenreUiState> = combine(genre, sort, networkMonitor.isOnline) { genre, sort, online ->
        GenreUiState(title = genre?.name, filter = route.mediaFilter, sort = sort, isOffline = !online)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = GenreUiState(filter = route.mediaFilter),
    )

    init {
        viewModelScope.launch { genreRepository.refresh() }
    }

    fun onSortChange(sort: MediaSort) {
        savedStateHandle[SORT_KEY] = sort.name
    }

    private companion object {
        const val SORT_KEY = "sort"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

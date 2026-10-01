package ru.kinopolka.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.repository.DetailsRepository
import ru.kinopolka.core.data.repository.LibraryRepository
import ru.kinopolka.core.data.util.NetworkMonitor
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.navigation.DetailsRoute

data class DetailsUiState(
    val key: MediaKey,
    val details: MediaDetails? = null,
    val entry: LibraryEntry? = null,
    val isRefreshing: Boolean = true,
    val error: DataError? = null,
    val isOffline: Boolean = false,
) {
    val content: DetailsContent
        get() = when {
            details != null -> DetailsContent.DATA
            isRefreshing -> DetailsContent.LOADING
            else -> DetailsContent.ERROR
        }

    val watchStatus: WatchStatus get() = entry?.watchStatus ?: WatchStatus.NONE
    val isFavorite: Boolean get() = entry?.isFavorite == true
}

enum class DetailsContent {
    LOADING,
    DATA,
    ERROR,
}

/**
 * Title card (F-08…F-11): the cached card is shown at once, stale data (24 h) is refreshed;
 * «Хочу посмотреть», «Смотрел» and «В избранное» change the library.
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val detailsRepository: DetailsRepository,
    private val libraryRepository: LibraryRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private data class RefreshState(val isRefreshing: Boolean = true, val error: DataError? = null)

    private val key: MediaKey = savedStateHandle.toRoute<DetailsRoute>().mediaKey

    private val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val refreshState = MutableStateFlow(RefreshState())
    private val retryRequests = MutableStateFlow(0)

    val uiState: StateFlow<DetailsUiState> = combine(
        detailsRepository.observeDetails(key),
        libraryRepository.observeEntry(key),
        refreshState,
        isOnline,
    ) { details, entry, refresh, online ->
        DetailsUiState(
            key = key,
            details = details,
            entry = entry,
            isRefreshing = refresh.isRefreshing,
            error = refresh.error,
            isOffline = !online,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DetailsUiState(key = key),
    )

    init {
        viewModelScope.launch {
            combine(isOnline, retryRequests) { online, _ -> online }.collectLatest { online ->
                if (online) {
                    refreshState.value = RefreshState(isRefreshing = true)
                    val result = detailsRepository.refreshDetails(key)
                    refreshState.value =
                        RefreshState(isRefreshing = false, error = (result as? RefreshResult.Failed)?.error)
                } else {
                    refreshState.value = RefreshState(isRefreshing = false, error = DataError.NO_CONNECTION)
                }
            }
        }
    }

    /** The selected status is cleared by a second tap; «Хочу» and «Смотрел» exclude each other. */
    fun onStatusClick(status: WatchStatus) {
        val current = uiState.value.watchStatus
        viewModelScope.launch {
            libraryRepository.setWatchStatus(key, if (current == status) WatchStatus.NONE else status)
        }
    }

    fun onFavoriteClick() {
        val favorite = !uiState.value.isFavorite
        viewModelScope.launch { libraryRepository.setFavorite(key, favorite) }
    }

    fun retry() {
        retryRequests.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

package ru.kinopolka.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.library.forTab
import ru.kinopolka.core.data.library.tabCounts
import ru.kinopolka.core.data.repository.LibraryRepository
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibrarySort
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaFilter

/** A removal that can still be undone from the Snackbar (F-13). */
data class PendingUndo(val entry: LibraryEntry, val title: String, val tab: LibraryTab)

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.WANT,
    val sort: LibrarySort = LibrarySort.ADDED,
    val filter: MediaFilter = MediaFilter.ALL,
    val items: List<LibraryItem> = emptyList(),
    val counts: Map<LibraryTab, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val pendingUndo: PendingUndo? = null,
)

/**
 * «Моя полка» (F-12, F-13): reads only the local database, so it works offline (N-01).
 * Tab, sort and filter live in [SavedStateHandle].
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val tab: Flow<LibraryTab> = savedStateHandle.enumFlow(TAB_KEY, LibraryTab.WANT)
    private val sort: Flow<LibrarySort> = savedStateHandle.enumFlow(SORT_KEY, LibrarySort.ADDED)
    private val filter: Flow<MediaFilter> = savedStateHandle.enumFlow(FILTER_KEY, MediaFilter.ALL)
    private val pendingUndo = MutableStateFlow<PendingUndo?>(null)

    private val settings = combine(tab, sort, filter) { tab, sort, filter -> Triple(tab, sort, filter) }

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeItems(),
        settings,
        pendingUndo,
    ) { items, (tab, sort, filter), undo ->
        LibraryUiState(
            tab = tab,
            sort = sort,
            filter = filter,
            items = items.forTab(tab, filter, sort),
            counts = items.tabCounts(filter),
            isLoading = false,
            pendingUndo = undo,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryUiState(),
    )

    fun onTabChange(tab: LibraryTab) {
        savedStateHandle[TAB_KEY] = tab.name
    }

    fun onSortChange(sort: LibrarySort) {
        savedStateHandle[SORT_KEY] = sort.name
    }

    fun onFilterChange(filter: MediaFilter) {
        savedStateHandle[FILTER_KEY] = filter.name
    }

    /** Removes the title from the current tab; «Отменить» brings it back. */
    fun onRemove(item: LibraryItem) {
        val tab = uiState.value.tab
        viewModelScope.launch {
            // Only the last removal can be undone: the previous one becomes final.
            pendingUndo.value?.let { libraryRepository.releasePoster(it.entry) }
            val previous = libraryRepository.removeFromTab(item.entry.key, tab)
            pendingUndo.value = previous?.let { PendingUndo(it, item.media.title, tab) }
        }
    }

    fun onUndo() {
        val undo = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { libraryRepository.restore(undo.entry) }
    }

    fun onUndoDismissed() {
        val undo = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { libraryRepository.releasePoster(undo.entry) }
    }

    private companion object {
        const val TAB_KEY = "tab"
        const val SORT_KEY = "sort"
        const val FILTER_KEY = "filter"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private inline fun <reified T : Enum<T>> SavedStateHandle.enumFlow(key: String, default: T): Flow<T> =
    getStateFlow(key, default.name).map { name -> enumValues<T>().firstOrNull { it.name == name } ?: default }

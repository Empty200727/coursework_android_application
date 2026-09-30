package ru.kinopolka.feature.home

import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.Shelf

data class ShelfContent(val shelf: Shelf, val items: List<Media>)

data class HomeUiState(
    val filter: MediaFilter = MediaFilter.ALL,
    val shelves: List<ShelfContent> = emptyList(),
    val isRefreshing: Boolean = true,
    /** Last refresh error; cached shelves stay visible. */
    val error: DataError? = null,
    val isOffline: Boolean = false,
) {
    val content: HomeContent
        get() = when {
            shelves.any { it.items.isNotEmpty() } -> HomeContent.DATA
            isRefreshing -> HomeContent.LOADING
            error != null -> HomeContent.ERROR
            else -> HomeContent.EMPTY
        }
}

/** Screen state variants (docs/PLAN.md, section 7): loading, data, empty, error. */
enum class HomeContent {
    LOADING,
    DATA,
    EMPTY,
    ERROR,
}

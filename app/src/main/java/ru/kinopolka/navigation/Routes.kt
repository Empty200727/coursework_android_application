package ru.kinopolka.navigation

import kotlinx.serialization.Serializable
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType

// Type-safe Navigation Compose routes (docs/PLAN.md, section 7). Arguments are primitive
// values; the typed getters convert them back to the domain types.

/** Nested graphs of the bottom navigation tabs: each tab keeps its own back stack. */
@Serializable
data object HomeGraph

@Serializable
data object SearchGraph

@Serializable
data object LibraryGraph

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object LibraryRoute

@Serializable
data object AboutRoute

/** Title card: only the type and id are passed, the data comes from the repository. */
@Serializable
data class DetailsRoute(val mediaTypeKey: String, val id: Int) {
    constructor(key: MediaKey) : this(key.mediaType.key, key.tmdbId)

    val mediaType: MediaType get() = MediaType.fromKey(mediaTypeKey) ?: MediaType.MOVIE
    val mediaKey: MediaKey get() = MediaKey(mediaType, id)
}

/** Full list of a genre from the unified catalog. */
@Serializable
data class GenreRoute(val genreKey: String, val filter: String = MediaFilter.ALL.name) {
    constructor(genreKey: String, filter: MediaFilter) : this(genreKey, filter.name)

    val mediaFilter: MediaFilter get() = MediaFilter.entries.firstOrNull { it.name == filter } ?: MediaFilter.ALL
}

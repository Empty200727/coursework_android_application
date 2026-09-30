package ru.kinopolka.core.model

/** A genre exactly as TMDB returns it: ids differ between movies and series. */
data class TmdbGenre(val mediaType: MediaType, val id: Int, val name: String)

/**
 * A genre of the unified catalog (F-04). The user sees one genre while the app substitutes
 * the matching TMDB id for each media type; a missing id means the genre does not exist
 * for that type (e.g. «Ужасы» has no series counterpart).
 */
data class Genre(val key: String, val name: String, val movieGenreId: Int?, val tvGenreId: Int?) {
    fun idFor(type: MediaType): Int? = when (type) {
        MediaType.MOVIE -> movieGenreId
        MediaType.TV -> tvGenreId
    }

    /** Media types for which this genre can be requested. */
    val mediaTypes: Set<MediaType>
        get() = MediaType.entries.filterTo(mutableSetOf()) { idFor(it) != null }

    /** Whether the genre has content for at least one type allowed by [filter]. */
    fun matches(filter: MediaFilter): Boolean = filter.mediaTypes.any { idFor(it) != null }
}

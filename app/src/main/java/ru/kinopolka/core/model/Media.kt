package ru.kinopolka.core.model

import java.time.LocalDate

/** A movie or a series as shown in lists, shelves and search results. */
data class Media(
    val key: MediaKey,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: LocalDate?,
    val voteAverage: Double,
    val voteCount: Int,
    val popularity: Double,
    val genreIds: List<Int>,
) {
    val mediaType: MediaType get() = key.mediaType
    val tmdbId: Int get() = key.tmdbId

    /** Year of release (first air date for series), `null` when TMDB has no date. */
    val releaseYear: Int? get() = releaseDate?.year
}

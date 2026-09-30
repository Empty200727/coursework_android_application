package ru.kinopolka.feature.home

import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf

/** Genres of the home shelves in display order; genres missing for the filter are skipped. */
internal val HOME_GENRE_KEYS = listOf(
    "action",
    "comedy",
    "drama",
    "animation",
    "science_fiction",
    "crime",
    "horror",
    "family",
    "mystery",
    "documentary",
)

/** F-05: 6–8 genre shelves. */
internal const val MAX_GENRE_SHELVES = 8

/**
 * Shelves of the home screen for [filter] (F-05, F-07): trending, popular movies and/or
 * series, then genre shelves available for the filter.
 */
internal fun homeShelves(filter: MediaFilter, genres: List<Genre>): List<Shelf> = buildList {
    add(Shelf.Trending(filter))
    if (filter.includes(MediaType.MOVIE)) add(Shelf.Popular(MediaType.MOVIE))
    if (filter.includes(MediaType.TV)) add(Shelf.Popular(MediaType.TV))
    val byKey = genres.associateBy { it.key }
    HOME_GENRE_KEYS
        .mapNotNull { byKey[it] }
        .filter { it.matches(filter) }
        .take(MAX_GENRE_SHELVES)
        .forEach { add(Shelf.ByGenre(it, filter)) }
}

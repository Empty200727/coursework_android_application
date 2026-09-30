package ru.kinopolka.core.data

import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.network.DiscoverSort
import ru.kinopolka.core.network.TmdbApi

/**
 * Merges movie and series lists for the «Все» filter: one list ordered by [sort],
 * without duplicates, at most [limit] items.
 */
fun mergeMedia(
    lists: List<List<Media>>,
    sort: MediaSort = MediaSort.POPULARITY,
    limit: Int = TmdbApi.PAGE_SIZE,
): List<Media> = lists.flatten()
    .distinctBy { it.key }
    .sortedWith(sort.comparator)
    .take(limit)

/** Descending order used by TMDB discover for the same sort. */
val MediaSort.comparator: Comparator<Media>
    get() = when (this) {
        MediaSort.POPULARITY -> compareByDescending { it.popularity }
        MediaSort.RATING -> compareByDescending<Media> { it.voteAverage }.thenByDescending { it.voteCount }
        MediaSort.NEWEST -> compareByDescending(nullsFirst()) { it.releaseDate }
    }

fun mapMediaSort(sort: MediaSort): DiscoverSort = when (sort) {
    MediaSort.POPULARITY -> DiscoverSort.POPULARITY
    MediaSort.RATING -> DiscoverSort.RATING
    MediaSort.NEWEST -> DiscoverSort.NEWEST
}

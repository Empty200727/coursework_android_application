package ru.kinopolka.core.model

import java.time.Instant

/**
 * «Хочу посмотреть» and «Смотрел» are mutually exclusive, so they are one status field;
 * «Избранное» is an independent flag (see the status rule in docs/PLAN.md, section 2).
 */
enum class WatchStatus {
    NONE,
    WANT,
    WATCHED,
}

/** A title in the user's library («Моя полка»). */
data class LibraryEntry(
    val key: MediaKey,
    val watchStatus: WatchStatus,
    val isFavorite: Boolean,
    val addedAt: Instant,
    val watchedAt: Instant?,
    val userRating: Int?,
    val localPosterPath: String?,
) {
    /** An entry with no status and no favorite flag must be removed from the database. */
    val isEmpty: Boolean get() = watchStatus == WatchStatus.NONE && !isFavorite
}

/** A library entry with the cached title data for «Моя полка». */
data class LibraryItem(val entry: LibraryEntry, val media: Media)

/** Tabs of «Моя полка» (F-12). */
enum class LibraryTab {
    WANT,
    WATCHED,
    FAVORITES,
    ;

    fun contains(entry: LibraryEntry): Boolean = when (this) {
        WANT -> entry.watchStatus == WatchStatus.WANT
        WATCHED -> entry.watchStatus == WatchStatus.WATCHED
        FAVORITES -> entry.isFavorite
    }
}

/** Sort orders of «Моя полка» (F-12). */
enum class LibrarySort {
    ADDED,
    TITLE,
    RATING,
}

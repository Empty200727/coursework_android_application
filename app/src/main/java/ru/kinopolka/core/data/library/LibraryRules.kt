package ru.kinopolka.core.data.library

import java.text.Collator
import java.time.Instant
import java.util.Locale
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibrarySort
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.WatchStatus

// Status rule (docs/PLAN.md, section 2): «Хочу посмотреть» and «Смотрел» are one status field,
// so they exclude each other; «Избранное» is an independent flag. An entry with no status and
// no flag is removed.

private fun newEntry(key: MediaKey, now: Instant) = LibraryEntry(
    key = key,
    watchStatus = WatchStatus.NONE,
    isFavorite = false,
    addedAt = now,
    watchedAt = null,
    userRating = null,
    localPosterPath = null,
)

/** Sets [status]; «Смотрел» records the date, setting a new status moves the title to the top of its tab. */
fun LibraryEntry?.withWatchStatus(key: MediaKey, status: WatchStatus, now: Instant): LibraryEntry {
    val entry = this ?: newEntry(key, now)
    if (entry.watchStatus == status) return entry
    return entry.copy(
        watchStatus = status,
        addedAt = if (status == WatchStatus.NONE) entry.addedAt else now,
        watchedAt = if (status == WatchStatus.WATCHED) now else null,
    )
}

fun LibraryEntry?.withFavorite(key: MediaKey, favorite: Boolean, now: Instant): LibraryEntry {
    val entry = this ?: newEntry(key, now)
    return entry.copy(isFavorite = favorite)
}

/** F-13: removal from a tab clears only what this tab shows. */
fun LibraryEntry.removedFrom(tab: LibraryTab): LibraryEntry = when (tab) {
    LibraryTab.WANT, LibraryTab.WATCHED -> copy(watchStatus = WatchStatus.NONE, watchedAt = null)
    LibraryTab.FAVORITES -> copy(isFavorite = false)
}

private val russianCollator: Collator = Collator.getInstance(Locale.forLanguageTag("ru"))

/** Items of [tab] filtered by type and sorted (F-12). */
fun List<LibraryItem>.forTab(tab: LibraryTab, filter: MediaFilter, sort: LibrarySort): List<LibraryItem> {
    val items = filter { tab.contains(it.entry) && filter.includes(it.media.mediaType) }
    return when (sort) {
        LibrarySort.ADDED -> items.sortedByDescending { it.entry.addedAt }

        LibrarySort.TITLE -> items.sortedWith(compareBy(russianCollator) { it.media.title })

        LibrarySort.RATING -> items.sortedWith(
            compareByDescending<LibraryItem> { it.media.voteAverage }.thenByDescending { it.media.voteCount },
        )
    }
}

/** Counters of the tabs for the selected type filter. */
fun List<LibraryItem>.tabCounts(filter: MediaFilter): Map<LibraryTab, Int> = LibraryTab.entries.associateWith { tab ->
    count { tab.contains(it.entry) && filter.includes(it.media.mediaType) }
}

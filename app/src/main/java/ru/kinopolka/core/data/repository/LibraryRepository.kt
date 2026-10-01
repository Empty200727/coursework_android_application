package ru.kinopolka.core.data.repository

import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.WatchStatus

/** «Моя полка» (F-11…F-13): stored only on the device and available offline (N-01). */
interface LibraryRepository {

    fun observeEntry(key: MediaKey): Flow<LibraryEntry?>

    /** All entries with the cached data of their titles. */
    fun observeItems(): Flow<List<LibraryItem>>

    /** The title must already be stored in `media` (it is, once its card was opened). */
    suspend fun setWatchStatus(key: MediaKey, status: WatchStatus)

    suspend fun setFavorite(key: MediaKey, favorite: Boolean)

    /**
     * Removes the title from [tab] and returns the entry as it was, for «Отменить».
     * The saved poster is kept until [releasePoster].
     */
    suspend fun removeFromTab(key: MediaKey, tab: LibraryTab): LibraryEntry?

    /** Puts back an entry returned by [removeFromTab]. */
    suspend fun restore(entry: LibraryEntry)

    /** Deletes the saved poster of [entry] if the title is no longer in the library. */
    suspend fun releasePoster(entry: LibraryEntry)
}

package ru.kinopolka.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.Shelf

/** Catalog of titles: home shelves cached in Room, search and genre lists paged from TMDB. */
interface MediaRepository {

    /** Titles of [shelf] from the local database; empty until the first successful refresh. */
    fun observeShelf(shelf: Shelf): Flow<List<Media>>

    /** Loads [shelf] from TMDB if it was never loaded or is older than 6 hours (N-03), or if [force]. */
    suspend fun refreshShelf(shelf: Shelf, force: Boolean = false): RefreshResult

    /** Search results (F-01…F-03); not cached in Room. */
    fun search(query: String, filter: MediaFilter): Flow<PagingData<Media>>

    /** Full list of a genre (F-06); not cached in Room. */
    fun genreMedia(genre: Genre, filter: MediaFilter, sort: MediaSort): Flow<PagingData<Media>>
}

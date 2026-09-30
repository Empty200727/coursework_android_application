package ru.kinopolka.testing

import androidx.paging.PagingData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.util.NetworkMonitor
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf

class FakeGenreRepository(genres: List<Genre> = emptyList()) : GenreRepository {
    val genres = MutableStateFlow(genres)
    val names = MutableStateFlow<Map<MediaType, Map<Int, String>>>(emptyMap())

    /** Completed by the test to finish a pending refresh. */
    var nextResult = CompletableDeferred<RefreshResult>(RefreshResult.Skipped)
    var refreshCalls = 0
        private set

    override fun observeGenres(): Flow<List<Genre>> = genres

    override suspend fun getGenre(key: String): Genre? = genres.value.firstOrNull { it.key == key }

    override fun observeGenreNames(): Flow<Map<MediaType, Map<Int, String>>> = names

    override suspend fun refresh(force: Boolean): RefreshResult {
        refreshCalls++
        return nextResult.await()
    }
}

class FakeMediaRepository : MediaRepository {
    val shelves = MutableStateFlow<Map<Shelf, List<Media>>>(emptyMap())
    val refreshedShelves = mutableListOf<Shelf>()
    var refreshResult: RefreshResult = RefreshResult.Updated

    /** Called on refresh, e.g. to put data into [shelves]. */
    var onRefresh: (Shelf) -> Unit = {}

    /** Every search that was started: query and filter. */
    val searches = mutableListOf<Pair<String, MediaFilter>>()
    var searchResults: (query: String, filter: MediaFilter) -> Flow<PagingData<Media>> =
        { _, _ -> flowOf(PagingData.empty()) }

    override fun observeShelf(shelf: Shelf): Flow<List<Media>> = shelves.map { it[shelf].orEmpty() }

    override suspend fun refreshShelf(shelf: Shelf, force: Boolean): RefreshResult {
        refreshedShelves += shelf
        onRefresh(shelf)
        return refreshResult
    }

    override fun search(query: String, filter: MediaFilter): Flow<PagingData<Media>> {
        searches += query to filter
        return searchResults(query, filter)
    }

    override fun genreMedia(genre: Genre, filter: MediaFilter, sort: MediaSort): Flow<PagingData<Media>> =
        flowOf(PagingData.empty())
}

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}

package ru.kinopolka.testing

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.library.PosterStorage
import ru.kinopolka.core.data.library.removedFrom
import ru.kinopolka.core.data.library.withFavorite
import ru.kinopolka.core.data.library.withWatchStatus
import ru.kinopolka.core.data.repository.DetailsRepository
import ru.kinopolka.core.data.repository.LibraryRepository
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.WatchStatus

/** In-memory library following the same status rule as the real repository. */
class FakeLibraryRepository(private val clock: TestTimeProvider = TestTimeProvider()) : LibraryRepository {
    val entries = MutableStateFlow<Map<MediaKey, LibraryEntry>>(emptyMap())

    /** Cached titles that library entries refer to. */
    val media = MutableStateFlow<Map<MediaKey, Media>>(emptyMap())
    val releasedPosters = mutableListOf<LibraryEntry>()

    private fun now() = Instant.ofEpochMilli(clock.nowMillis())

    private fun put(key: MediaKey, entry: LibraryEntry) {
        entries.value = if (entry.isEmpty) entries.value - key else entries.value + (key to entry)
    }

    override fun observeEntry(key: MediaKey): Flow<LibraryEntry?> = entries.map { it[key] }

    override fun observeItems(): Flow<List<LibraryItem>> = combine(entries, media) { entries, media ->
        entries.values.mapNotNull { entry -> media[entry.key]?.let { LibraryItem(entry, it) } }
    }

    override suspend fun setWatchStatus(key: MediaKey, status: WatchStatus) {
        put(key, entries.value[key].withWatchStatus(key, status, now()))
    }

    override suspend fun setFavorite(key: MediaKey, favorite: Boolean) {
        put(key, entries.value[key].withFavorite(key, favorite, now()))
    }

    override suspend fun removeFromTab(key: MediaKey, tab: LibraryTab): LibraryEntry? {
        val entry = entries.value[key] ?: return null
        put(key, entry.removedFrom(tab))
        return entry
    }

    override suspend fun restore(entry: LibraryEntry) {
        put(entry.key, entry)
    }

    override suspend fun releasePoster(entry: LibraryEntry) {
        releasedPosters += entry
    }
}

class FakeDetailsRepository : DetailsRepository {
    val details = MutableStateFlow<Map<MediaKey, MediaDetails>>(emptyMap())
    var refreshResult: RefreshResult = RefreshResult.Updated
    val refreshed = mutableListOf<MediaKey>()

    override fun observeDetails(key: MediaKey): Flow<MediaDetails?> = details.map { it[key] }

    override suspend fun refreshDetails(key: MediaKey, force: Boolean): RefreshResult {
        refreshed += key
        return refreshResult
    }
}

/** Keeps "saved" posters in memory: the path is `posters/<type>_<id>.jpg`. */
class FakePosterStorage : PosterStorage {
    val files = mutableSetOf<String>()
    var failSaving = false

    override suspend fun save(key: MediaKey, posterPath: String): String? {
        if (failSaving) return null
        return "posters/${key.mediaType.key}_${key.tmdbId}.jpg".also { files += it }
    }

    override fun exists(path: String): Boolean = path in files

    override suspend fun delete(path: String) {
        files -= path
    }

    override suspend fun deleteAllExcept(keep: Set<String>): Int {
        val removed = files - keep
        files.removeAll(removed)
        return removed.size
    }
}

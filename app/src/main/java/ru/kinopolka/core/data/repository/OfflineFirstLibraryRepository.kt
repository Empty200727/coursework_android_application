package ru.kinopolka.core.data.repository

import androidx.room.withTransaction
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.library.PosterStorage
import ru.kinopolka.core.data.library.removedFrom
import ru.kinopolka.core.data.library.withFavorite
import ru.kinopolka.core.data.library.withWatchStatus
import ru.kinopolka.core.data.mapper.toEntity
import ru.kinopolka.core.data.mapper.toLibraryEntry
import ru.kinopolka.core.data.mapper.toMedia
import ru.kinopolka.core.database.KinopolkaDatabase
import ru.kinopolka.core.database.dao.LibraryDao
import ru.kinopolka.core.database.dao.MediaDao
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.WatchStatus

@Singleton
class OfflineFirstLibraryRepository @Inject constructor(
    private val database: KinopolkaDatabase,
    private val libraryDao: LibraryDao,
    private val mediaDao: MediaDao,
    private val posterStorage: PosterStorage,
    private val timeProvider: TimeProvider,
) : LibraryRepository {

    override fun observeEntry(key: MediaKey): Flow<LibraryEntry?> =
        libraryDao.observeEntry(key.mediaType, key.tmdbId).map { it?.toLibraryEntry() }.distinctUntilChanged()

    override fun observeItems(): Flow<List<LibraryItem>> =
        combine(libraryDao.observeAll(), mediaDao.observeLibraryMedia()) { entries, media ->
            val mediaByKey = media.associateBy { MediaKey(it.mediaType, it.tmdbId) }
            entries.mapNotNull { entity ->
                val entry = entity.toLibraryEntry()
                mediaByKey[entry.key]?.let { LibraryItem(entry, it.toMedia()) }
            }
        }.distinctUntilChanged()

    override suspend fun setWatchStatus(key: MediaKey, status: WatchStatus) {
        update(key) { it.withWatchStatus(key, status, now()) }
    }

    override suspend fun setFavorite(key: MediaKey, favorite: Boolean) {
        update(key) { it.withFavorite(key, favorite, now()) }
    }

    override suspend fun removeFromTab(key: MediaKey, tab: LibraryTab): LibraryEntry? {
        var previous: LibraryEntry? = null
        update(key, keepPosterOnDelete = true) { entry ->
            previous = entry
            requireNotNull(entry) { "$key is not in the library" }.removedFrom(tab)
        }
        return previous
    }

    override suspend fun restore(entry: LibraryEntry) {
        val poster = entry.localPosterPath?.takeIf { posterStorage.exists(it) }
        update(entry.key) { entry.copy(localPosterPath = poster) }
    }

    override suspend fun releasePoster(entry: LibraryEntry) {
        val path = entry.localPosterPath ?: return
        if (libraryDao.get(entry.key.mediaType, entry.key.tmdbId) == null) posterStorage.delete(path)
    }

    /**
     * Applies [transform] to the stored entry in a transaction. An empty result deletes the
     * entry together with its poster file; a new entry gets its poster saved.
     */
    private suspend fun update(
        key: MediaKey,
        keepPosterOnDelete: Boolean = false,
        transform: (LibraryEntry?) -> LibraryEntry,
    ) {
        val (old, new) = database.withTransaction {
            val old = libraryDao.get(key.mediaType, key.tmdbId)?.toLibraryEntry()
            val new = transform(old)
            if (new.isEmpty) {
                libraryDao.delete(key.mediaType, key.tmdbId)
            } else {
                libraryDao.upsert(new.toEntity())
            }
            old to new
        }
        when {
            new.isEmpty -> if (!keepPosterOnDelete) old?.localPosterPath?.let { posterStorage.delete(it) }
            new.localPosterPath == null -> savePoster(key)
        }
    }

    private suspend fun savePoster(key: MediaKey) {
        val posterPath = mediaDao.get(key.mediaType, key.tmdbId)?.posterPath ?: return
        val file = posterStorage.save(key, posterPath) ?: return
        libraryDao.updatePosterPath(key.mediaType, key.tmdbId, file)
    }

    private fun now(): Instant = Instant.ofEpochMilli(timeProvider.nowMillis())
}

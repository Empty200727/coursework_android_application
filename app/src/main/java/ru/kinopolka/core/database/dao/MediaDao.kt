package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.MediaEntity
import ru.kinopolka.core.database.entity.MediaGenreEntity
import ru.kinopolka.core.database.entity.MediaSummaryUpdate
import ru.kinopolka.core.database.entity.toSummaryUpdate
import ru.kinopolka.core.model.MediaType

/**
 * Never use [OnConflictStrategy.REPLACE] for `media`: it deletes the row first and the
 * foreign keys cascade to cast, genres, related titles and feeds.
 */
@Dao
interface MediaDao {

    @Query("SELECT * FROM media WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    fun observe(mediaType: MediaType, tmdbId: Int): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun get(mediaType: MediaType, tmdbId: Int): MediaEntity?

    /** Writes full rows, e.g. from the title card. */
    @Upsert
    suspend fun upsert(media: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(media: List<MediaEntity>)

    @Update(entity = MediaEntity::class)
    suspend fun updateSummaries(summaries: List<MediaSummaryUpdate>)

    /**
     * Writes rows from list responses: new titles are inserted, known titles get only
     * the list columns updated, so previously loaded details survive.
     */
    @Transaction
    suspend fun upsertSummaries(media: List<MediaEntity>) {
        insertIgnoringExisting(media)
        updateSummaries(media.map { it.toSummaryUpdate() })
    }

    @Query("SELECT genre_id FROM media_genre WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    fun observeGenreIds(mediaType: MediaType, tmdbId: Int): Flow<List<Int>>

    @Query("DELETE FROM media_genre WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun deleteGenres(mediaType: MediaType, tmdbId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(genres: List<MediaGenreEntity>)

    @Transaction
    suspend fun replaceGenres(mediaType: MediaType, tmdbId: Int, genreIds: List<Int>) {
        deleteGenres(mediaType, tmdbId)
        insertGenres(genreIds.distinct().map { MediaGenreEntity(mediaType, tmdbId, it) })
    }

    /**
     * Cache cleanup: removes titles cached before [threshold] that are not in the library.
     * Cast, genres, related titles and feed items go away by cascade.
     */
    @Query(
        """
        DELETE FROM media
        WHERE cached_at < :threshold
          AND NOT EXISTS (
              SELECT 1 FROM library_entry
              WHERE library_entry.media_type = media.media_type
                AND library_entry.tmdb_id = media.tmdb_id
          )
        """,
    )
    suspend fun deleteUnusedCachedBefore(threshold: Long): Int

    @Query("SELECT COUNT(*) FROM media")
    suspend fun count(): Int
}

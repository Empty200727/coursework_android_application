package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    fun observeEntry(mediaType: MediaType, tmdbId: Int): Flow<LibraryEntryEntity?>

    @Query("SELECT * FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun get(mediaType: MediaType, tmdbId: Int): LibraryEntryEntity?

    @Query("SELECT * FROM library_entry ORDER BY added_at DESC")
    fun observeAll(): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entry WHERE watch_status = :status ORDER BY added_at DESC")
    fun observeByStatus(status: WatchStatus): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entry WHERE is_favorite = 1 ORDER BY added_at DESC")
    fun observeFavorites(): Flow<List<LibraryEntryEntity>>

    @Upsert
    suspend fun upsert(entry: LibraryEntryEntity)

    @Query("DELETE FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun delete(mediaType: MediaType, tmdbId: Int)

    @Query(
        """
        UPDATE library_entry SET local_poster_path = :path
        WHERE media_type = :mediaType AND tmdb_id = :tmdbId
        """,
    )
    suspend fun updatePosterPath(mediaType: MediaType, tmdbId: Int, path: String?)

    /** Posters saved for the library (N-01); files not listed here can be deleted. */
    @Query("SELECT local_poster_path FROM library_entry WHERE local_poster_path IS NOT NULL")
    suspend fun posterPaths(): List<String>
}

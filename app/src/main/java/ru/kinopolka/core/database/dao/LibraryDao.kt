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

    @Query("SELECT * FROM library_entry WHERE watch_status = :status ORDER BY added_at DESC")
    fun observeByStatus(status: WatchStatus): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entry WHERE is_favorite = 1 ORDER BY added_at DESC")
    fun observeFavorites(): Flow<List<LibraryEntryEntity>>

    @Upsert
    suspend fun upsert(entry: LibraryEntryEntity)

    @Query("DELETE FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun delete(mediaType: MediaType, tmdbId: Int)
}

package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.GenreEntity
import ru.kinopolka.core.model.MediaType

@Dao
interface GenreDao {

    @Query("SELECT * FROM genre ORDER BY media_type, genre_id")
    fun observeAll(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genre ORDER BY media_type, genre_id")
    suspend fun getAll(): List<GenreEntity>

    /** Oldest write time for [mediaType] or `null` when there are no genres of that type. */
    @Query("SELECT MIN(cached_at) FROM genre WHERE media_type = :mediaType")
    suspend fun oldestCachedAt(mediaType: MediaType): Long?

    @Query("DELETE FROM genre WHERE media_type = :mediaType")
    suspend fun deleteByType(mediaType: MediaType)

    @Upsert
    suspend fun upsert(genres: List<GenreEntity>)

    /** Replaces the genre list of one type atomically: removed TMDB genres disappear. */
    @Transaction
    suspend fun replaceForType(mediaType: MediaType, genres: List<GenreEntity>) {
        deleteByType(mediaType)
        upsert(genres)
    }
}

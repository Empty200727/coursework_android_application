package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.CastMemberEntity
import ru.kinopolka.core.model.MediaType

@Dao
interface CastDao {

    @Query(
        "SELECT * FROM cast_member WHERE media_type = :mediaType AND tmdb_id = :tmdbId ORDER BY cast_order",
    )
    fun observeCast(mediaType: MediaType, tmdbId: Int): Flow<List<CastMemberEntity>>

    @Query("DELETE FROM cast_member WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    suspend fun deleteCast(mediaType: MediaType, tmdbId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCast(cast: List<CastMemberEntity>)

    @Transaction
    suspend fun replaceCast(mediaType: MediaType, tmdbId: Int, cast: List<CastMemberEntity>) {
        deleteCast(mediaType, tmdbId)
        insertCast(cast)
    }
}

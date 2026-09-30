package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.MediaEntity
import ru.kinopolka.core.database.entity.RelatedMediaEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind

@Dao
interface RelatedMediaDao {

    @Query(
        """
        SELECT media.* FROM related_media
        INNER JOIN media
            ON media.media_type = related_media.target_media_type
           AND media.tmdb_id = related_media.target_tmdb_id
        WHERE related_media.source_media_type = :mediaType
          AND related_media.source_tmdb_id = :tmdbId
          AND related_media.kind = :kind
        ORDER BY related_media.position
        """,
    )
    fun observeRelated(mediaType: MediaType, tmdbId: Int, kind: RelatedKind): Flow<List<MediaEntity>>

    @Query(
        """
        DELETE FROM related_media
        WHERE source_media_type = :mediaType AND source_tmdb_id = :tmdbId AND kind = :kind
        """,
    )
    suspend fun deleteRelated(mediaType: MediaType, tmdbId: Int, kind: RelatedKind)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelated(related: List<RelatedMediaEntity>)

    /** Target titles must already be stored in `media`. */
    @Transaction
    suspend fun replaceRelated(
        mediaType: MediaType,
        tmdbId: Int,
        kind: RelatedKind,
        related: List<RelatedMediaEntity>,
    ) {
        deleteRelated(mediaType, tmdbId, kind)
        insertRelated(related)
    }
}

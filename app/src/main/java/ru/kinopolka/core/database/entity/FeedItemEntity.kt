package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ru.kinopolka.core.model.MediaType

/**
 * Cached first page of a home screen collection or shelf, e.g. `trending` or
 * `genre:action:all`. Stale after 6 hours (N-03).
 */
@Entity(
    tableName = "feed_item",
    primaryKeys = ["feed_key", "position"],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["media_type", "tmdb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["media_type", "tmdb_id"])],
)
data class FeedItemEntity(
    @ColumnInfo(name = "feed_key") val feedKey: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

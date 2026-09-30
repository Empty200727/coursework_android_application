package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus

/**
 * The user's library. Unlike the cache it must never be deleted implicitly: the foreign key
 * uses RESTRICT, so a cached `media` row referenced here cannot be removed.
 */
@Entity(
    tableName = "library_entry",
    primaryKeys = ["media_type", "tmdb_id"],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["media_type", "tmdb_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class LibraryEntryEntity(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "watch_status") val watchStatus: WatchStatus,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "watched_at") val watchedAt: Long?,
    @ColumnInfo(name = "user_rating") val userRating: Int?,
    @ColumnInfo(name = "local_poster_path") val localPosterPath: String?,
)

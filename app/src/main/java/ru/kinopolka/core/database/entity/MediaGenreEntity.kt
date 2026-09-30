package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import ru.kinopolka.core.model.MediaType

/** Many-to-many link between a title and TMDB genre ids of its type. */
@Entity(
    tableName = "media_genre",
    primaryKeys = ["media_type", "tmdb_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["media_type", "tmdb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MediaGenreEntity(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "genre_id") val genreId: Int,
)

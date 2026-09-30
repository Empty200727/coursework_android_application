package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import ru.kinopolka.core.model.MediaType

/** TMDB genre list for one media type; cached for 7 days (N-03). */
@Entity(
    tableName = "genre",
    primaryKeys = ["media_type", "genre_id"],
)
data class GenreEntity(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "genre_id") val genreId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
)

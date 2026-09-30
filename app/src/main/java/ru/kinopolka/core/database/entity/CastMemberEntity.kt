package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import ru.kinopolka.core.model.MediaType

@Entity(
    tableName = "cast_member",
    primaryKeys = ["media_type", "tmdb_id", "person_id"],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["media_type", "tmdb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CastMemberEntity(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "person_id") val personId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "character") val character: String?,
    @ColumnInfo(name = "profile_path") val profilePath: String?,
    @ColumnInfo(name = "cast_order") val castOrder: Int,
)

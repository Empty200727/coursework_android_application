package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind

/** Recommendations and similar titles of a source title; both ends are rows of `media`. */
@Entity(
    tableName = "related_media",
    primaryKeys = [
        "source_media_type",
        "source_tmdb_id",
        "kind",
        "target_media_type",
        "target_tmdb_id",
    ],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["source_media_type", "source_tmdb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_type", "tmdb_id"],
            childColumns = ["target_media_type", "target_tmdb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["target_media_type", "target_tmdb_id"])],
)
data class RelatedMediaEntity(
    @ColumnInfo(name = "source_media_type") val sourceMediaType: MediaType,
    @ColumnInfo(name = "source_tmdb_id") val sourceTmdbId: Int,
    @ColumnInfo(name = "kind") val kind: RelatedKind,
    @ColumnInfo(name = "target_media_type") val targetMediaType: MediaType,
    @ColumnInfo(name = "target_tmdb_id") val targetTmdbId: Int,
    @ColumnInfo(name = "position") val position: Int,
)

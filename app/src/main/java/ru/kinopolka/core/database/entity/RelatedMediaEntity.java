package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;

/** Recommendations and similar titles of a source title; both ends are rows of {@code media}. */
@Entity(
        tableName = "related_media",
        primaryKeys = {"source_media_type", "source_tmdb_id", "kind", "target_media_type", "target_tmdb_id"},
        foreignKeys = {
            @ForeignKey(
                    entity = MediaEntity.class,
                    parentColumns = {"media_type", "tmdb_id"},
                    childColumns = {"source_media_type", "source_tmdb_id"},
                    onDelete = ForeignKey.CASCADE),
            @ForeignKey(
                    entity = MediaEntity.class,
                    parentColumns = {"media_type", "tmdb_id"},
                    childColumns = {"target_media_type", "target_tmdb_id"},
                    onDelete = ForeignKey.CASCADE),
        },
        indices = @Index(value = {"target_media_type", "target_tmdb_id"}))
public class RelatedMediaEntity {
    @ColumnInfo(name = "source_media_type") @NonNull public final MediaType sourceMediaType;
    @ColumnInfo(name = "source_tmdb_id") public final int sourceTmdbId;
    @ColumnInfo(name = "kind") @NonNull public final RelatedKind kind;
    @ColumnInfo(name = "target_media_type") @NonNull public final MediaType targetMediaType;
    @ColumnInfo(name = "target_tmdb_id") public final int targetTmdbId;
    @ColumnInfo(name = "position") public final int position;

    public RelatedMediaEntity(@NonNull MediaType sourceMediaType, int sourceTmdbId, @NonNull RelatedKind kind,
            @NonNull MediaType targetMediaType, int targetTmdbId, int position) {
        this.sourceMediaType = sourceMediaType;
        this.sourceTmdbId = sourceTmdbId;
        this.kind = kind;
        this.targetMediaType = targetMediaType;
        this.targetTmdbId = targetTmdbId;
        this.position = position;
    }
}

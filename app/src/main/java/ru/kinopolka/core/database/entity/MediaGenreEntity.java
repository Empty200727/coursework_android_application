package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import ru.kinopolka.core.model.MediaType;

/** Many-to-many link between a title and TMDB genre ids of its type. */
@Entity(
        tableName = "media_genre",
        primaryKeys = {"media_type", "tmdb_id", "genre_id"},
        foreignKeys = @ForeignKey(
                entity = MediaEntity.class,
                parentColumns = {"media_type", "tmdb_id"},
                childColumns = {"media_type", "tmdb_id"},
                onDelete = ForeignKey.CASCADE))
public class MediaGenreEntity {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "genre_id") public final int genreId;

    public MediaGenreEntity(@NonNull MediaType mediaType, int tmdbId, int genreId) {
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.genreId = genreId;
    }
}

package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import ru.kinopolka.core.model.MediaType;

/** TMDB genre list for one media type; cached for 7 days (N-03). */
@Entity(tableName = "genre", primaryKeys = {"media_type", "genre_id"})
public class GenreEntity {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "genre_id") public final int genreId;
    @ColumnInfo(name = "name") @NonNull public final String name;
    @ColumnInfo(name = "cached_at") public final long cachedAt;

    public GenreEntity(@NonNull MediaType mediaType, int genreId, @NonNull String name, long cachedAt) {
        this.mediaType = mediaType;
        this.genreId = genreId;
        this.name = name;
        this.cachedAt = cachedAt;
    }
}

package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;

/**
 * The user's library. Unlike the cache it must never be deleted implicitly: the foreign key
 * uses RESTRICT, so a cached {@code media} row referenced here cannot be removed.
 */
@Entity(
        tableName = "library_entry",
        primaryKeys = {"media_type", "tmdb_id"},
        foreignKeys = @ForeignKey(
                entity = MediaEntity.class,
                parentColumns = {"media_type", "tmdb_id"},
                childColumns = {"media_type", "tmdb_id"},
                onDelete = ForeignKey.RESTRICT))
public class LibraryEntryEntity {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "watch_status") @NonNull public final WatchStatus watchStatus;
    @ColumnInfo(name = "is_favorite") public final boolean isFavorite;
    @ColumnInfo(name = "added_at") public final long addedAt;
    @ColumnInfo(name = "watched_at") @Nullable public final Long watchedAt;
    @ColumnInfo(name = "user_rating") @Nullable public final Integer userRating;
    @ColumnInfo(name = "local_poster_path") @Nullable public final String localPosterPath;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public LibraryEntryEntity(@NonNull MediaType mediaType, int tmdbId, @NonNull WatchStatus watchStatus,
            boolean isFavorite, long addedAt, @Nullable Long watchedAt, @Nullable Integer userRating,
            @Nullable String localPosterPath) {
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.watchStatus = watchStatus;
        this.isFavorite = isFavorite;
        this.addedAt = addedAt;
        this.watchedAt = watchedAt;
        this.userRating = userRating;
        this.localPosterPath = localPosterPath;
    }
}

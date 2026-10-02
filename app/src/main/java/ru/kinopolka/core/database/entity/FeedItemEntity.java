package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import ru.kinopolka.core.model.MediaType;

/**
 * Cached first page of a home screen collection or shelf, e.g. {@code trending:all} or
 * {@code genre:28:movie}. Stale after 6 hours (N-03).
 */
@Entity(
        tableName = "feed_item",
        primaryKeys = {"feed_key", "position"},
        foreignKeys = @ForeignKey(
                entity = MediaEntity.class,
                parentColumns = {"media_type", "tmdb_id"},
                childColumns = {"media_type", "tmdb_id"},
                onDelete = ForeignKey.CASCADE),
        indices = @Index(value = {"media_type", "tmdb_id"}))
public class FeedItemEntity {
    @ColumnInfo(name = "feed_key") @NonNull public final String feedKey;
    @ColumnInfo(name = "position") public final int position;
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "fetched_at") public final long fetchedAt;

    public FeedItemEntity(@NonNull String feedKey, int position, @NonNull MediaType mediaType, int tmdbId,
            long fetchedAt) {
        this.feedKey = feedKey;
        this.position = position;
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.fetchedAt = fetchedAt;
    }
}

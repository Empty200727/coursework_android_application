package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import ru.kinopolka.core.model.MediaType;

/**
 * Columns known from list responses. Updating a row through this class keeps the details
 * (runtime, seasons, overview fallback) loaded by the title card.
 */
public class MediaSummaryUpdate {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "title") @NonNull public final String title;
    @ColumnInfo(name = "original_title") @Nullable public final String originalTitle;
    @ColumnInfo(name = "poster_path") @Nullable public final String posterPath;
    @ColumnInfo(name = "backdrop_path") @Nullable public final String backdropPath;
    @ColumnInfo(name = "release_date") @Nullable public final String releaseDate;
    @ColumnInfo(name = "vote_average") public final double voteAverage;
    @ColumnInfo(name = "vote_count") public final int voteCount;
    @ColumnInfo(name = "popularity") public final double popularity;
    @ColumnInfo(name = "cached_at") public final long cachedAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public MediaSummaryUpdate(@NonNull MediaType mediaType, int tmdbId, @NonNull String title,
            @Nullable String originalTitle, @Nullable String posterPath, @Nullable String backdropPath,
            @Nullable String releaseDate, double voteAverage, int voteCount, double popularity, long cachedAt) {
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
        this.popularity = popularity;
        this.cachedAt = cachedAt;
    }
}

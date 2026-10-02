package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import ru.kinopolka.core.model.MediaType;

/**
 * Cache of title metadata. Rows can be deleted without losing user data, except rows
 * referenced by {@code library_entry}. Dates are ISO-8601 strings, times are epoch milliseconds.
 */
@Entity(tableName = "media", primaryKeys = {"media_type", "tmdb_id"})
public class MediaEntity {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "title") @NonNull public final String title;
    @ColumnInfo(name = "original_title") @Nullable public final String originalTitle;
    @ColumnInfo(name = "overview") @Nullable public final String overview;
    @ColumnInfo(name = "is_overview_fallback", defaultValue = "0") public final boolean isOverviewFallback;
    @ColumnInfo(name = "poster_path") @Nullable public final String posterPath;
    @ColumnInfo(name = "backdrop_path") @Nullable public final String backdropPath;
    @ColumnInfo(name = "release_date") @Nullable public final String releaseDate;
    @ColumnInfo(name = "last_air_date") @Nullable public final String lastAirDate;
    @ColumnInfo(name = "vote_average") public final double voteAverage;
    @ColumnInfo(name = "vote_count") public final int voteCount;
    @ColumnInfo(name = "popularity") public final double popularity;
    /** Movie runtime in minutes. */
    @ColumnInfo(name = "runtime") @Nullable public final Integer runtime;
    @ColumnInfo(name = "number_of_seasons") @Nullable public final Integer numberOfSeasons;
    @ColumnInfo(name = "in_production") @Nullable public final Boolean inProduction;
    /** Last time the row was written from any source; drives the 30-day cleanup. */
    @ColumnInfo(name = "cached_at") public final long cachedAt;
    /** Last time full details were loaded, {@code null} if only list data is known (N-03, 24 h). */
    @ColumnInfo(name = "details_cached_at") @Nullable public final Long detailsCachedAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public MediaEntity(@NonNull MediaType mediaType, int tmdbId, @NonNull String title, @Nullable String originalTitle,
            @Nullable String overview, boolean isOverviewFallback, @Nullable String posterPath,
            @Nullable String backdropPath, @Nullable String releaseDate, @Nullable String lastAirDate,
            double voteAverage, int voteCount, double popularity, @Nullable Integer runtime,
            @Nullable Integer numberOfSeasons, @Nullable Boolean inProduction, long cachedAt,
            @Nullable Long detailsCachedAt) {
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.isOverviewFallback = isOverviewFallback;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.releaseDate = releaseDate;
        this.lastAirDate = lastAirDate;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
        this.popularity = popularity;
        this.runtime = runtime;
        this.numberOfSeasons = numberOfSeasons;
        this.inProduction = inProduction;
        this.cachedAt = cachedAt;
        this.detailsCachedAt = detailsCachedAt;
    }

    /** Columns known from list responses (see {@link MediaSummaryUpdate}). */
    public MediaSummaryUpdate toSummaryUpdate() {
        return new MediaSummaryUpdate(mediaType, tmdbId, title, originalTitle, posterPath, backdropPath, releaseDate,
                voteAverage, voteCount, popularity, cachedAt);
    }
}

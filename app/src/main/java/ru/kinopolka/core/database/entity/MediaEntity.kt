package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import ru.kinopolka.core.model.MediaType

/**
 * Cache of title metadata. Rows can be deleted without losing user data, except rows
 * referenced by `library_entry`.
 *
 * Dates are ISO-8601 strings, times are epoch milliseconds.
 */
@Entity(
    tableName = "media",
    primaryKeys = ["media_type", "tmdb_id"],
)
data class MediaEntity(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String?,
    @ColumnInfo(name = "overview") val overview: String?,
    @ColumnInfo(name = "is_overview_fallback", defaultValue = "0") val isOverviewFallback: Boolean,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    @ColumnInfo(name = "release_date") val releaseDate: String?,
    @ColumnInfo(name = "last_air_date") val lastAirDate: String?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double,
    @ColumnInfo(name = "vote_count") val voteCount: Int,
    @ColumnInfo(name = "popularity") val popularity: Double,
    /** Movie runtime in minutes. */
    @ColumnInfo(name = "runtime") val runtime: Int?,
    @ColumnInfo(name = "number_of_seasons") val numberOfSeasons: Int?,
    @ColumnInfo(name = "in_production") val inProduction: Boolean?,
    /** Last time the row was written from any source; drives the 30-day cleanup. */
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
    /** Last time full details were loaded, `null` if only list data is known (N-03, 24 h). */
    @ColumnInfo(name = "details_cached_at") val detailsCachedAt: Long?,
)

/**
 * Columns known from list responses. Updating a row through this class keeps the details
 * (runtime, seasons, overview fallback) loaded by the title card.
 */
data class MediaSummaryUpdate(
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    @ColumnInfo(name = "release_date") val releaseDate: String?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double,
    @ColumnInfo(name = "vote_count") val voteCount: Int,
    @ColumnInfo(name = "popularity") val popularity: Double,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
)

fun MediaEntity.toSummaryUpdate() = MediaSummaryUpdate(
    mediaType = mediaType,
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    cachedAt = cachedAt,
)

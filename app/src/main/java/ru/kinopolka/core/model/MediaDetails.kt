package ru.kinopolka.core.model

import java.time.LocalDate

/** Full information for the title card (F-08…F-10). */
data class MediaDetails(
    val media: Media,
    val genres: List<TmdbGenre>,
    /** Movie runtime in minutes, `null` for series. */
    val runtimeMinutes: Int?,
    /** Number of seasons, `null` for movies. */
    val numberOfSeasons: Int?,
    /** Last air date of a series, `null` for movies. */
    val lastAirDate: LocalDate?,
    /** Whether a series is still running, `null` for movies. */
    val inProduction: Boolean?,
    /** `true` when the Russian overview was missing and the English one is shown (N-07). */
    val isOverviewFallback: Boolean,
    val cast: List<CastMember>,
    val recommendations: List<Media>,
    val similar: List<Media>,
)

/** Actor in the title card (F-09). */
data class CastMember(
    val personId: Int,
    val name: String,
    val character: String?,
    val profilePath: String?,
    val order: Int,
)

/** Kind of relation between two titles (F-10). */
enum class RelatedKind {
    RECOMMENDATION,
    SIMILAR,
}

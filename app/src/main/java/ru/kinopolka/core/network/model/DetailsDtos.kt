package ru.kinopolka.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** movie/{id}?append_to_response=credits,recommendations,similar */
@Serializable
data class MovieDetailsDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("genres") val genres: List<GenreDto>? = null,
    @SerialName("credits") val credits: CreditsDto? = null,
    @SerialName("recommendations") val recommendations: PagedResponseDto<MediaListItemDto>? = null,
    @SerialName("similar") val similar: PagedResponseDto<MediaListItemDto>? = null,
)

/** tv/{id}?append_to_response=aggregate_credits,recommendations,similar */
@Serializable
data class TvDetailsDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("in_production") val inProduction: Boolean? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("genres") val genres: List<GenreDto>? = null,
    @SerialName("aggregate_credits") val aggregateCredits: AggregateCreditsDto? = null,
    @SerialName("recommendations") val recommendations: PagedResponseDto<MediaListItemDto>? = null,
    @SerialName("similar") val similar: PagedResponseDto<MediaListItemDto>? = null,
)

@Serializable
data class CreditsDto(@SerialName("cast") val cast: List<CastDto>? = null)

@Serializable
data class CastDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("character") val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("order") val order: Int? = null,
)

/** Series credits summed over all seasons: an actor may have several roles. */
@Serializable
data class AggregateCreditsDto(@SerialName("cast") val cast: List<AggregateCastDto>? = null)

@Serializable
data class AggregateCastDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("roles") val roles: List<RoleDto>? = null,
    @SerialName("total_episode_count") val totalEpisodeCount: Int? = null,
    @SerialName("order") val order: Int? = null,
)

@Serializable
data class RoleDto(
    @SerialName("character") val character: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
)

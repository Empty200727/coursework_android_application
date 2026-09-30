package ru.kinopolka.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// All DTO fields are nullable with defaults: TMDB omits fields freely and may change the
// response format, a partial answer must still be parsed (risk «TMDB меняет формат ответа»).

@Serializable
data class PagedResponseDto<T>(
    @SerialName("page") val page: Int? = null,
    @SerialName("results") val results: List<T>? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_results") val totalResults: Int? = null,
)

@Serializable
data class GenreDto(@SerialName("id") val id: Int? = null, @SerialName("name") val name: String? = null)

@Serializable
data class GenreListResponseDto(@SerialName("genres") val genres: List<GenreDto>? = null)

/**
 * An item of search/multi, trending, popular and discover results. Movies use `title`,
 * `original_title` and `release_date`, series use `name`, `original_name` and `first_air_date`.
 * `media_type` is present only in search/multi and trending (and can be `person`).
 */
@Serializable
data class MediaListItemDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int>? = null,
    @SerialName("adult") val adult: Boolean? = null,
)

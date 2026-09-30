package ru.kinopolka.core.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.PagedResponseDto
import ru.kinopolka.core.network.model.TvDetailsDto

/**
 * TMDB API v3, all endpoints from docs/PLAN.md, section 4.
 *
 * The bearer token and `language=ru-RU` are added by [TmdbRequestInterceptor]; a request may
 * pass its own [language] (e.g. `en-US` when the Russian overview is missing, N-07).
 */
interface TmdbApi {

    @GET("genre/movie/list")
    suspend fun getMovieGenres(): GenreListResponseDto

    @GET("genre/tv/list")
    suspend fun getTvGenres(): GenreListResponseDto

    /** Movies, series and people in one request; people are filtered out by the mapper (F-01). */
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MediaListItemDto>

    /** Search limited to one type, used by the «Фильмы» and «Сериалы» filters (F-02). */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MediaListItemDto>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MediaListItemDto>

    /** «В тренде за неделю» (F-07); [mediaType] is `all`, `movie` or `tv`. */
    @GET("trending/{media_type}/week")
    suspend fun getTrendingWeek(
        @Path("media_type") mediaType: String = TRENDING_ALL,
        @Query("page") page: Int = 1,
    ): PagedResponseDto<MediaListItemDto>

    @GET("movie/popular")
    suspend fun getPopularMovies(@Query("page") page: Int = 1): PagedResponseDto<MediaListItemDto>

    @GET("tv/popular")
    suspend fun getPopularTv(@Query("page") page: Int = 1): PagedResponseDto<MediaListItemDto>

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = DiscoverSort.POPULARITY.movieValue,
        @Query("vote_count.gte") minVoteCount: Int = DEFAULT_MIN_VOTE_COUNT,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MediaListItemDto>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = DiscoverSort.POPULARITY.tvValue,
        @Query("vote_count.gte") minVoteCount: Int = DEFAULT_MIN_VOTE_COUNT,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MediaListItemDto>

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("append_to_response") appendToResponse: String = MOVIE_APPEND_TO_RESPONSE,
        @Query("language") language: String? = null,
    ): MovieDetailsDto

    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("append_to_response") appendToResponse: String = TV_APPEND_TO_RESPONSE,
        @Query("language") language: String? = null,
    ): TvDetailsDto

    companion object {
        /** Hides titles rated 10 by two votes from shelves and genre screens. */
        const val DEFAULT_MIN_VOTE_COUNT = 100
        const val MOVIE_APPEND_TO_RESPONSE = "credits,recommendations,similar"
        const val TV_APPEND_TO_RESPONSE = "aggregate_credits,recommendations,similar"
        const val FALLBACK_LANGUAGE = "en-US"
        const val TRENDING_ALL = "all"

        /** TMDB returns 20 items per page and at most 500 pages. */
        const val PAGE_SIZE = 20
        const val MAX_PAGE = 500
    }
}

/** Sort orders of discover; release date fields differ for movies and series. */
enum class DiscoverSort(val movieValue: String, val tvValue: String) {
    POPULARITY("popularity.desc", "popularity.desc"),
    RATING("vote_average.desc", "vote_average.desc"),
    NEWEST("primary_release_date.desc", "first_air_date.desc"),
}

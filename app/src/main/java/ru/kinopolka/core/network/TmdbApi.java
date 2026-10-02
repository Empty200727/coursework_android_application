package ru.kinopolka.core.network;

import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.TvDetailsDto;

/**
 * TMDB API v3, all endpoints from docs/PLAN.md, section 4. Calls are executed synchronously on a
 * background executor through {@link TmdbCalls#execute}.
 *
 * <p>The bearer token and {@code language=ru-RU} are added by {@link TmdbRequestInterceptor}; a
 * request may pass its own {@code language} (e.g. {@code en-US} when the Russian overview is
 * missing, N-07).
 */
public interface TmdbApi {

    /** Hides titles rated 10 by two votes from shelves and genre screens. */
    int DEFAULT_MIN_VOTE_COUNT = 100;
    String MOVIE_APPEND_TO_RESPONSE = "credits,recommendations,similar";
    String TV_APPEND_TO_RESPONSE = "aggregate_credits,recommendations,similar";
    String FALLBACK_LANGUAGE = "en-US";
    String TRENDING_ALL = "all";
    /** TMDB returns 20 items per page and at most 500 pages. */
    int PAGE_SIZE = 20;
    int MAX_PAGE = 500;

    @GET("genre/movie/list")
    Call<GenreListResponseDto> getMovieGenres();

    @GET("genre/tv/list")
    Call<GenreListResponseDto> getTvGenres();

    /** Movies, series and people in one request; people are filtered out by the mapper (F-01). */
    @GET("search/multi")
    Call<PagedResponseDto<MediaListItemDto>> searchMulti(
            @Query("query") String query, @Query("page") int page, @Query("include_adult") boolean includeAdult);

    /** Search limited to one type, used by the «Фильмы» and «Сериалы» filters (F-02). */
    @GET("search/movie")
    Call<PagedResponseDto<MediaListItemDto>> searchMovies(
            @Query("query") String query, @Query("page") int page, @Query("include_adult") boolean includeAdult);

    @GET("search/tv")
    Call<PagedResponseDto<MediaListItemDto>> searchTv(
            @Query("query") String query, @Query("page") int page, @Query("include_adult") boolean includeAdult);

    /** «В тренде за неделю» (F-07); {@code mediaType} is {@code all}, {@code movie} or {@code tv}. */
    @GET("trending/{media_type}/week")
    Call<PagedResponseDto<MediaListItemDto>> getTrendingWeek(
            @Path("media_type") String mediaType, @Query("page") int page);

    @GET("movie/popular")
    Call<PagedResponseDto<MediaListItemDto>> getPopularMovies(@Query("page") int page);

    @GET("tv/popular")
    Call<PagedResponseDto<MediaListItemDto>> getPopularTv(@Query("page") int page);

    @GET("discover/movie")
    Call<PagedResponseDto<MediaListItemDto>> discoverMovies(
            @Query("with_genres") int genreId,
            @Query("sort_by") String sortBy,
            @Query("vote_count.gte") int minVoteCount,
            @Query("page") int page,
            @Query("include_adult") boolean includeAdult);

    @GET("discover/tv")
    Call<PagedResponseDto<MediaListItemDto>> discoverTv(
            @Query("with_genres") int genreId,
            @Query("sort_by") String sortBy,
            @Query("vote_count.gte") int minVoteCount,
            @Query("page") int page,
            @Query("include_adult") boolean includeAdult);

    /** {@code appendToResponse == null} loads the base card only. */
    @GET("movie/{id}")
    Call<MovieDetailsDto> getMovieDetails(
            @Path("id") int id,
            @Query("append_to_response") @Nullable String appendToResponse,
            @Query("language") @Nullable String language);

    @GET("tv/{id}")
    Call<TvDetailsDto> getTvDetails(
            @Path("id") int id,
            @Query("append_to_response") @Nullable String appendToResponse,
            @Query("language") @Nullable String language);
}

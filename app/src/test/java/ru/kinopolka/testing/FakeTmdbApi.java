package ru.kinopolka.testing;

import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.mock.Calls;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.TvDetailsDto;

/**
 * {@link TmdbApi} answering from fixtures or from test handlers. Every call is recorded in
 * {@link #requests} as {@code endpoint:arguments}.
 */
public final class FakeTmdbApi implements TmdbApi {

    public interface TrendingHandler {
        PagedResponseDto<MediaListItemDto> page(String mediaType, int page);
    }

    public interface PageHandler {
        PagedResponseDto<MediaListItemDto> page(int page);
    }

    public interface DiscoverHandler {
        PagedResponseDto<MediaListItemDto> page(int genreId, String sortBy, int page);
    }

    public interface SearchHandler {
        PagedResponseDto<MediaListItemDto> page(String endpoint, String query, int page);
    }

    public interface MovieDetailsHandler {
        MovieDetailsDto details(int id, @Nullable String language);
    }

    public interface TvDetailsHandler {
        TvDetailsDto details(int id, @Nullable String language);
    }

    public GenreListResponseDto movieGenres = Fixtures.parse("genre_movie_list.json", GenreListResponseDto.class);
    public GenreListResponseDto tvGenres = Fixtures.parse("genre_tv_list.json", GenreListResponseDto.class);
    public TrendingHandler trending = (mediaType, page) -> Fixtures.page("trending_all_week.json");
    public PageHandler popularMovies = page -> Fixtures.page("movie_popular.json");
    public PageHandler popularTv = page -> Fixtures.page("tv_popular.json");
    public DiscoverHandler discoverMoviesPage = (genreId, sortBy, page) -> Fixtures.page("discover_movie.json");
    public DiscoverHandler discoverTvPage = (genreId, sortBy, page) -> Fixtures.page("discover_tv.json");
    public SearchHandler search = (endpoint, query, page) -> Fixtures.page("search_multi.json");
    public MovieDetailsHandler movieDetails =
            (id, language) -> Fixtures.parse("movie_details_550.json", MovieDetailsDto.class);
    public TvDetailsHandler tvDetails = (id, language) -> Fixtures.parse("tv_details_1396.json", TvDetailsDto.class);

    /** Returned by every call when set: an {@link IOException} or an {@link HttpException}. */
    @Nullable
    public volatile Exception failure;

    public final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    public static HttpException httpError(int code) {
        return new HttpException(Response.error(code, ResponseBody.create("{}", MediaType.get("application/json"))));
    }

    public int genreRequests() {
        synchronized (requests) {
            int count = 0;
            for (String request : requests) {
                if (request.startsWith("genre/")) {
                    count++;
                }
            }
            return count;
        }
    }

    private <T> Call<T> answer(String request, java.util.function.Supplier<T> body) {
        requests.add(request);
        Exception error = failure;
        if (error instanceof HttpException http) {
            @SuppressWarnings("unchecked")
            Response<T> response = (Response<T>) http.response();
            return Calls.response(response);
        }
        if (error instanceof IOException io) {
            return Calls.failure(io);
        }
        if (error != null) {
            return Calls.failure(error);
        }
        return Calls.response(body.get());
    }

    @Override
    public Call<GenreListResponseDto> getMovieGenres() {
        return answer("genre/movie/list", () -> movieGenres);
    }

    @Override
    public Call<GenreListResponseDto> getTvGenres() {
        return answer("genre/tv/list", () -> tvGenres);
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> searchMulti(String query, int page, boolean includeAdult) {
        return answer("search/multi:" + query + ":" + page, () -> search.page("multi", query, page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> searchMovies(String query, int page, boolean includeAdult) {
        return answer("search/movie:" + query + ":" + page, () -> search.page("movie", query, page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> searchTv(String query, int page, boolean includeAdult) {
        return answer("search/tv:" + query + ":" + page, () -> search.page("tv", query, page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> getTrendingWeek(String mediaType, int page) {
        return answer("trending/" + mediaType + "/week", () -> trending.page(mediaType, page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> getPopularMovies(int page) {
        return answer("movie/popular", () -> popularMovies.page(page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> getPopularTv(int page) {
        return answer("tv/popular", () -> popularTv.page(page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> discoverMovies(int genreId, String sortBy, int minVoteCount,
            int page, boolean includeAdult) {
        return answer("discover/movie:" + genreId + ":" + sortBy + ":" + page,
                () -> discoverMoviesPage.page(genreId, sortBy, page));
    }

    @Override
    public Call<PagedResponseDto<MediaListItemDto>> discoverTv(int genreId, String sortBy, int minVoteCount,
            int page, boolean includeAdult) {
        return answer("discover/tv:" + genreId + ":" + sortBy + ":" + page,
                () -> discoverTvPage.page(genreId, sortBy, page));
    }

    @Override
    public Call<MovieDetailsDto> getMovieDetails(int id, @Nullable String appendToResponse, @Nullable String language) {
        return answer("movie/" + id + ":" + orEmpty(appendToResponse) + ":" + orEmpty(language),
                () -> movieDetails.details(id, language));
    }

    @Override
    public Call<TvDetailsDto> getTvDetails(int id, @Nullable String appendToResponse, @Nullable String language) {
        return answer("tv/" + id + ":" + orEmpty(appendToResponse) + ":" + orEmpty(language),
                () -> tvDetails.details(id, language));
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}

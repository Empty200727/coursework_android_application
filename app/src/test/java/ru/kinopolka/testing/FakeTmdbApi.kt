package ru.kinopolka.testing

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import ru.kinopolka.core.network.TmdbApi
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.PagedResponseDto
import ru.kinopolka.core.network.model.TvDetailsDto

private typealias Page = PagedResponseDto<MediaListItemDto>

/**
 * [TmdbApi] answering from fixtures or from test handlers. Every call is recorded in
 * [requests] as `endpoint:arguments`.
 */
class FakeTmdbApi : TmdbApi {
    var movieGenres: GenreListResponseDto = Fixtures.parse("genre_movie_list.json")
    var tvGenres: GenreListResponseDto = Fixtures.parse("genre_tv_list.json")

    var trending: suspend (mediaType: String, page: Int) -> Page = { _, _ -> Fixtures.parse("trending_all_week.json") }
    var popularMovies: suspend (page: Int) -> Page = { Fixtures.parse("movie_popular.json") }
    var popularTv: suspend (page: Int) -> Page = { Fixtures.parse("tv_popular.json") }
    var discoverMoviesPage: suspend (genreId: Int, sortBy: String, page: Int) -> Page =
        { _, _, _ -> Fixtures.parse("discover_movie.json") }
    var discoverTvPage: suspend (genreId: Int, sortBy: String, page: Int) -> Page =
        { _, _, _ -> Fixtures.parse("discover_tv.json") }
    var search: suspend (endpoint: String, query: String, page: Int) -> Page =
        { _, _, _ -> Fixtures.parse("search_multi.json") }

    /** Thrown by every call when set. */
    var failure: Throwable? = null

    val requests = mutableListOf<String>()

    val genreRequests: Int get() = requests.count { it.startsWith("genre/") }

    private fun record(request: String) {
        requests += request
        failure?.let { throw it }
    }

    override suspend fun getMovieGenres(): GenreListResponseDto {
        record("genre/movie/list")
        return movieGenres
    }

    override suspend fun getTvGenres(): GenreListResponseDto {
        record("genre/tv/list")
        return tvGenres
    }

    override suspend fun searchMulti(query: String, page: Int, includeAdult: Boolean): Page {
        record("search/multi:$query:$page")
        return search("multi", query, page)
    }

    override suspend fun searchMovies(query: String, page: Int, includeAdult: Boolean): Page {
        record("search/movie:$query:$page")
        return search("movie", query, page)
    }

    override suspend fun searchTv(query: String, page: Int, includeAdult: Boolean): Page {
        record("search/tv:$query:$page")
        return search("tv", query, page)
    }

    override suspend fun getTrendingWeek(mediaType: String, page: Int): Page {
        record("trending/$mediaType/week")
        return trending(mediaType, page)
    }

    override suspend fun getPopularMovies(page: Int): Page {
        record("movie/popular")
        return popularMovies(page)
    }

    override suspend fun getPopularTv(page: Int): Page {
        record("tv/popular")
        return popularTv(page)
    }

    override suspend fun discoverMovies(
        genreId: Int,
        sortBy: String,
        minVoteCount: Int,
        page: Int,
        includeAdult: Boolean,
    ): Page {
        record("discover/movie:$genreId:$sortBy:$page")
        return discoverMoviesPage(genreId, sortBy, page)
    }

    override suspend fun discoverTv(
        genreId: Int,
        sortBy: String,
        minVoteCount: Int,
        page: Int,
        includeAdult: Boolean,
    ): Page {
        record("discover/tv:$genreId:$sortBy:$page")
        return discoverTvPage(genreId, sortBy, page)
    }

    override suspend fun getMovieDetails(id: Int, appendToResponse: String, language: String?): MovieDetailsDto =
        throw UnsupportedOperationException()

    override suspend fun getTvDetails(id: Int, appendToResponse: String, language: String?): TvDetailsDto =
        throw UnsupportedOperationException()

    companion object {
        fun httpError(code: Int): HttpException = HttpException(Response.error<Any>(code, "{}".toResponseBody()))
    }
}

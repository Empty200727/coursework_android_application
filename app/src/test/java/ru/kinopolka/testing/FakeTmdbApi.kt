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

/** [TmdbApi] answering the genre endpoints from fixtures; other endpoints are not used yet. */
class FakeTmdbApi : TmdbApi {
    var movieGenres: GenreListResponseDto = Fixtures.parse("genre_movie_list.json")
    var tvGenres: GenreListResponseDto = Fixtures.parse("genre_tv_list.json")

    /** Thrown by every call when set. */
    var failure: Throwable? = null

    var genreRequests = 0
        private set

    override suspend fun getMovieGenres(): GenreListResponseDto {
        genreRequests++
        failure?.let { throw it }
        return movieGenres
    }

    override suspend fun getTvGenres(): GenreListResponseDto {
        genreRequests++
        failure?.let { throw it }
        return tvGenres
    }

    override suspend fun searchMulti(query: String, page: Int, includeAdult: Boolean) = notUsed()

    override suspend fun getTrendingWeek(page: Int) = notUsed()

    override suspend fun getPopularMovies(page: Int) = notUsed()

    override suspend fun getPopularTv(page: Int) = notUsed()

    override suspend fun discoverMovies(
        genreId: Int,
        sortBy: String,
        minVoteCount: Int,
        page: Int,
        includeAdult: Boolean,
    ) = notUsed()

    override suspend fun discoverTv(
        genreId: Int,
        sortBy: String,
        minVoteCount: Int,
        page: Int,
        includeAdult: Boolean,
    ) = notUsed()

    override suspend fun getMovieDetails(id: Int, appendToResponse: String, language: String?): MovieDetailsDto =
        throw UnsupportedOperationException()

    override suspend fun getTvDetails(id: Int, appendToResponse: String, language: String?): TvDetailsDto =
        throw UnsupportedOperationException()

    private fun notUsed(): PagedResponseDto<MediaListItemDto> = throw UnsupportedOperationException()

    companion object {
        fun httpError(code: Int): HttpException = HttpException(Response.error<Any>(code, "{}".toResponseBody()))
    }
}

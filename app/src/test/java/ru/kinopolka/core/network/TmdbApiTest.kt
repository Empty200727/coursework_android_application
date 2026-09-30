package ru.kinopolka.core.network

import java.io.IOException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import ru.kinopolka.testing.Fixtures

/** Network layer against MockWebServer with TMDB response fixtures (docs/PLAN.md, section 8). */
class TmdbApiTest {

    private val server = MockWebServer()
    private val sleeps = mutableListOf<Long>()
    private lateinit var api: TmdbApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder()
            .addInterceptor(TmdbRequestInterceptor(token = "test-token"))
            .addInterceptor(RateLimitRetryInterceptor(sleep = { sleeps += it }))
            .build()
        api = createTmdbApi(server.url("/3/").toString(), client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueueFixture(name: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(Fixtures.read(name)).build())
    }

    @Test
    fun `adds bearer token and russian language to every request`() = runTest {
        enqueueFixture("genre_movie_list.json")

        api.getMovieGenres()

        val request = server.takeRequest()
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("ru-RU", request.url.queryParameter("language"))
        assertEquals("/3/genre/movie/list", request.url.encodedPath)
    }

    @Test
    fun `explicit language is not overridden`() = runTest {
        enqueueFixture("movie_details_550.json")

        api.getMovieDetails(550, language = TmdbApi.FALLBACK_LANGUAGE)

        val url = server.takeRequest().url
        assertEquals(listOf("en-US"), url.queryParameterValues("language"))
    }

    @Test
    fun `parses genre lists of both types`() = runTest {
        enqueueFixture("genre_movie_list.json")
        enqueueFixture("genre_tv_list.json")

        val movie = api.getMovieGenres().genres.orEmpty()
        val tv = api.getTvGenres().genres.orEmpty()

        assertEquals(19, movie.size)
        assertEquals("боевик", movie.first { it.id == 28 }.name)
        assertEquals(16, tv.size)
        assertEquals("Боевик и Приключения", tv.first { it.id == 10759 }.name)
        assertEquals("/3/genre/movie/list", server.takeRequest().url.encodedPath)
        assertEquals("/3/genre/tv/list", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `search multi sends query, page and include_adult and keeps people for the mapper`() = runTest {
        enqueueFixture("search_multi.json")

        val response = api.searchMulti(query = "во все тяжкие", page = 2)

        val url = server.takeRequest().url
        assertEquals("/3/search/multi", url.encodedPath)
        assertEquals("во все тяжкие", url.queryParameter("query"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("false", url.queryParameter("include_adult"))
        assertEquals(listOf("tv", "person", "movie"), response.results.orEmpty().map { it.mediaType })
        assertEquals(1, response.totalPages)
    }

    @Test
    fun `trending and popular endpoints use the documented paths`() = runTest {
        enqueueFixture("trending_all_week.json")
        enqueueFixture("movie_popular.json")
        enqueueFixture("tv_popular.json")

        val trending = api.getTrendingWeek()
        api.getPopularMovies(page = 3)
        val tv = api.getPopularTv()

        assertEquals("/3/trending/all/week", server.takeRequest().url.encodedPath)
        val popularMovies = server.takeRequest().url
        assertEquals("/3/movie/popular", popularMovies.encodedPath)
        assertEquals("3", popularMovies.queryParameter("page"))
        assertEquals("/3/tv/popular", server.takeRequest().url.encodedPath)
        assertEquals(2, trending.results.orEmpty().size)
        assertEquals("Во все тяжкие", tv.results.orEmpty().single().name)
    }

    @Test
    fun `discover sends genre, sort order and minimal vote count`() = runTest {
        enqueueFixture("discover_movie.json")
        enqueueFixture("discover_movie.json")

        val movies = api.discoverMovies(genreId = 28, sortBy = DiscoverSort.RATING.movieValue)
        api.discoverTv(genreId = 10759, sortBy = DiscoverSort.NEWEST.tvValue, page = 2)

        val movieUrl = server.takeRequest().url
        assertEquals("/3/discover/movie", movieUrl.encodedPath)
        assertEquals("28", movieUrl.queryParameter("with_genres"))
        assertEquals("vote_average.desc", movieUrl.queryParameter("sort_by"))
        assertEquals("100", movieUrl.queryParameter("vote_count.gte"))
        assertEquals("false", movieUrl.queryParameter("include_adult"))
        val tvUrl = server.takeRequest().url
        assertEquals("/3/discover/tv", tvUrl.encodedPath)
        assertEquals("first_air_date.desc", tvUrl.queryParameter("sort_by"))
        assertEquals("2", tvUrl.queryParameter("page"))
        assertEquals(603, movies.results.orEmpty().single().id)
    }

    @Test
    fun `movie details are loaded with credits, recommendations and similar in one request`() = runTest {
        enqueueFixture("movie_details_550.json")

        val details = api.getMovieDetails(550)

        val url = server.takeRequest().url
        assertEquals("/3/movie/550", url.encodedPath)
        assertEquals("credits,recommendations,similar", url.queryParameter("append_to_response"))
        assertEquals("Бойцовский клуб", details.title)
        assertEquals(139, details.runtime)
        assertEquals(12, details.credits?.cast?.size)
        assertEquals(2, details.recommendations?.results?.size)
        assertEquals(1, details.similar?.results?.size)
    }

    @Test
    fun `tv details are loaded with aggregate credits`() = runTest {
        enqueueFixture("tv_details_1396.json")

        val details = api.getTvDetails(1396)

        val url = server.takeRequest().url
        assertEquals("/3/tv/1396", url.encodedPath)
        assertEquals("aggregate_credits,recommendations,similar", url.queryParameter("append_to_response"))
        assertEquals(5, details.numberOfSeasons)
        assertEquals("2013-09-29", details.lastAirDate)
        assertEquals(2, details.aggregateCredits?.cast?.get(1)?.roles?.size)
        assertEquals(emptyList<Any>(), details.similar?.results)
    }

    @Test
    fun `missing fields are parsed as nulls`() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"id": 1, "unknown_field": {"a": 1}}""").build())

        val details = api.getMovieDetails(1)

        assertEquals(1, details.id)
        assertNull(details.title)
        assertNull(details.credits)
    }

    @Test
    fun `401 is reported as HttpException`() = runTest {
        enqueueFixture("error_401.json", code = 401)

        val error = runCatching { api.getMovieGenres() }.exceptionOrNull()

        assertTrue(error is HttpException)
        assertEquals(401, (error as HttpException).code())
    }

    @Test
    fun `404 is reported as HttpException`() = runTest {
        enqueueFixture("error_404.json", code = 404)

        val error = runCatching { api.getMovieDetails(999_999_999) }.exceptionOrNull()

        assertEquals(404, (error as HttpException).code())
    }

    @Test
    fun `429 is retried after Retry-After`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "2").build())
        enqueueFixture("genre_movie_list.json")

        val response = api.getMovieGenres()

        assertEquals(19, response.genres.orEmpty().size)
        assertEquals(2, server.requestCount)
        assertEquals(listOf(2_000L), sleeps)
    }

    @Test
    fun `429 retries use growing pauses and are limited`() = runTest {
        repeat(4) { server.enqueue(MockResponse.Builder().code(429).build()) }

        val error = runCatching { api.getMovieGenres() }.exceptionOrNull()

        assertEquals(429, (error as HttpException).code())
        assertEquals(4, server.requestCount)
        assertEquals(listOf(1_000L, 2_000L, 4_000L), sleeps)
    }

    @Test
    fun `no connection is reported as IOException`() = runTest {
        server.close()

        val error = runCatching { api.getMovieGenres() }.exceptionOrNull()

        assertTrue("Expected IOException, got $error", error is IOException)
    }
}

package ru.kinopolka.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.HttpException;
import ru.kinopolka.core.network.model.GenreDto;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.TvDetailsDto;
import ru.kinopolka.testing.Fixtures;

/** Network layer against MockWebServer with TMDB response fixtures (docs/PLAN.md, section 8). */
public class TmdbApiTest {

    private final MockWebServer server = new MockWebServer();
    private final List<Long> sleeps = new ArrayList<>();
    private TmdbApi api;

    @Before
    public void setUp() throws IOException {
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TmdbRequestInterceptor("test-token"))
                .addInterceptor(new RateLimitRetryInterceptor(RateLimitRetryInterceptor.DEFAULT_MAX_RETRIES,
                        RateLimitRetryInterceptor.DEFAULT_BASE_DELAY_MILLIS,
                        RateLimitRetryInterceptor.DEFAULT_MAX_DELAY_MILLIS, sleeps::add))
                .build();
        api = TmdbNetwork.createTmdbApi(server.url("/3/").toString(), client, TmdbNetwork.gson());
    }

    @After
    public void tearDown() {
        server.close();
    }

    private void enqueueFixture(String name) {
        enqueueFixture(name, 200);
    }

    private void enqueueFixture(String name, int code) {
        server.enqueue(new MockResponse.Builder().code(code).body(Fixtures.read(name)).build());
    }

    private HttpUrl takeUrl() throws InterruptedException {
        return server.takeRequest().getUrl();
    }

    @Test
    public void addsBearerTokenAndRussianLanguageToEveryRequest() throws Exception {
        enqueueFixture("genre_movie_list.json");

        TmdbCalls.execute(api.getMovieGenres());

        mockwebserver3.RecordedRequest request = server.takeRequest();
        assertEquals("Bearer test-token", request.getHeaders().get("Authorization"));
        assertEquals("ru-RU", request.getUrl().queryParameter("language"));
        assertEquals("/3/genre/movie/list", request.getUrl().encodedPath());
    }

    @Test
    public void explicitLanguageIsNotOverridden() throws Exception {
        enqueueFixture("movie_details_550.json");

        TmdbCalls.execute(api.getMovieDetails(550, null, TmdbApi.FALLBACK_LANGUAGE));

        assertEquals(List.of("en-US"), takeUrl().queryParameterValues("language"));
    }

    @Test
    public void parsesGenreListsOfBothTypes() throws Exception {
        enqueueFixture("genre_movie_list.json");
        enqueueFixture("genre_tv_list.json");

        List<GenreDto> movie = TmdbCalls.execute(api.getMovieGenres()).genres();
        List<GenreDto> tv = TmdbCalls.execute(api.getTvGenres()).genres();

        assertEquals(19, movie.size());
        assertEquals("боевик", movie.stream().filter(it -> it.id() == 28).findFirst().orElseThrow().name());
        assertEquals(16, tv.size());
        assertEquals("Боевик и Приключения",
                tv.stream().filter(it -> it.id() == 10759).findFirst().orElseThrow().name());
        assertEquals("/3/genre/movie/list", takeUrl().encodedPath());
        assertEquals("/3/genre/tv/list", takeUrl().encodedPath());
    }

    @Test
    public void searchMultiSendsQueryPageAndIncludeAdultAndKeepsPeopleForTheMapper() throws Exception {
        enqueueFixture("search_multi.json");

        PagedResponseDto<MediaListItemDto> response = TmdbCalls.execute(api.searchMulti("во все тяжкие", 2, false));

        HttpUrl url = takeUrl();
        assertEquals("/3/search/multi", url.encodedPath());
        assertEquals("во все тяжкие", url.queryParameter("query"));
        assertEquals("2", url.queryParameter("page"));
        assertEquals("false", url.queryParameter("include_adult"));
        assertEquals(List.of("tv", "person", "movie"),
                response.results().stream().map(MediaListItemDto::mediaType).toList());
        assertEquals(Integer.valueOf(1), response.totalPages());
    }

    @Test
    public void trendingAndPopularEndpointsUseTheDocumentedPaths() throws Exception {
        enqueueFixture("trending_all_week.json");
        enqueueFixture("movie_popular.json");
        enqueueFixture("tv_popular.json");

        PagedResponseDto<MediaListItemDto> trending =
                TmdbCalls.execute(api.getTrendingWeek(TmdbApi.TRENDING_ALL, 1));
        TmdbCalls.execute(api.getPopularMovies(3));
        PagedResponseDto<MediaListItemDto> tv = TmdbCalls.execute(api.getPopularTv(1));

        assertEquals("/3/trending/all/week", takeUrl().encodedPath());
        HttpUrl popularMovies = takeUrl();
        assertEquals("/3/movie/popular", popularMovies.encodedPath());
        assertEquals("3", popularMovies.queryParameter("page"));
        assertEquals("/3/tv/popular", takeUrl().encodedPath());
        assertEquals(2, trending.results().size());
        assertEquals("Во все тяжкие", tv.results().get(0).name());
    }

    @Test
    public void trendingAndSearchCanBeLimitedToOneType() throws Exception {
        enqueueFixture("trending_all_week.json");
        enqueueFixture("movie_popular.json");
        enqueueFixture("tv_popular.json");

        TmdbCalls.execute(api.getTrendingWeek("tv", 1));
        TmdbCalls.execute(api.searchMovies("матрица", 2, false));
        TmdbCalls.execute(api.searchTv("тяжкие", 1, false));

        assertEquals("/3/trending/tv/week", takeUrl().encodedPath());
        HttpUrl movieSearch = takeUrl();
        assertEquals("/3/search/movie", movieSearch.encodedPath());
        assertEquals("матрица", movieSearch.queryParameter("query"));
        assertEquals("2", movieSearch.queryParameter("page"));
        assertEquals("/3/search/tv", takeUrl().encodedPath());
    }

    @Test
    public void discoverSendsGenreSortOrderAndMinimalVoteCount() throws Exception {
        enqueueFixture("discover_movie.json");
        enqueueFixture("discover_movie.json");

        PagedResponseDto<MediaListItemDto> movies = TmdbCalls.execute(api.discoverMovies(28,
                DiscoverSort.RATING.movieValue(), TmdbApi.DEFAULT_MIN_VOTE_COUNT, 1, false));
        TmdbCalls.execute(api.discoverTv(10759, DiscoverSort.NEWEST.tvValue(), TmdbApi.DEFAULT_MIN_VOTE_COUNT, 2,
                false));

        HttpUrl movieUrl = takeUrl();
        assertEquals("/3/discover/movie", movieUrl.encodedPath());
        assertEquals("28", movieUrl.queryParameter("with_genres"));
        assertEquals("vote_average.desc", movieUrl.queryParameter("sort_by"));
        assertEquals("100", movieUrl.queryParameter("vote_count.gte"));
        assertEquals("false", movieUrl.queryParameter("include_adult"));
        HttpUrl tvUrl = takeUrl();
        assertEquals("/3/discover/tv", tvUrl.encodedPath());
        assertEquals("first_air_date.desc", tvUrl.queryParameter("sort_by"));
        assertEquals("2", tvUrl.queryParameter("page"));
        assertEquals(1, movies.results().size());
        assertEquals(Integer.valueOf(603), movies.results().get(0).id());
    }

    @Test
    public void movieDetailsAreLoadedWithCreditsRecommendationsAndSimilarInOneRequest() throws Exception {
        enqueueFixture("movie_details_550.json");

        MovieDetailsDto details = TmdbCalls.execute(api.getMovieDetails(550, TmdbApi.MOVIE_APPEND_TO_RESPONSE, null));

        HttpUrl url = takeUrl();
        assertEquals("/3/movie/550", url.encodedPath());
        assertEquals("credits,recommendations,similar", url.queryParameter("append_to_response"));
        assertEquals("Бойцовский клуб", details.title());
        assertEquals(Integer.valueOf(139), details.runtime());
        assertEquals(12, details.credits().cast().size());
        assertEquals(2, details.recommendations().results().size());
        assertEquals(1, details.similar().results().size());
    }

    @Test
    public void tvDetailsAreLoadedWithAggregateCredits() throws Exception {
        enqueueFixture("tv_details_1396.json");

        TvDetailsDto details = TmdbCalls.execute(api.getTvDetails(1396, TmdbApi.TV_APPEND_TO_RESPONSE, null));

        HttpUrl url = takeUrl();
        assertEquals("/3/tv/1396", url.encodedPath());
        assertEquals("aggregate_credits,recommendations,similar", url.queryParameter("append_to_response"));
        assertEquals(Integer.valueOf(5), details.numberOfSeasons());
        assertEquals("2013-09-29", details.lastAirDate());
        assertEquals(2, details.aggregateCredits().cast().get(1).roles().size());
        assertEquals(List.of(), details.similar().results());
    }

    @Test
    public void missingFieldsAreParsedAsNulls() throws Exception {
        server.enqueue(new MockResponse.Builder().body("{\"id\": 1, \"unknown_field\": {\"a\": 1}}").build());

        MovieDetailsDto details = TmdbCalls.execute(api.getMovieDetails(1, null, null));

        assertEquals(Integer.valueOf(1), details.id());
        assertNull(details.title());
        assertNull(details.credits());
    }

    @Test
    public void malformedBodyIsReportedAsJsonParseException() {
        server.enqueue(new MockResponse.Builder().body("{\"id\": \"not a number\"}").build());

        assertThrows(JsonParseException.class, () -> TmdbCalls.execute(api.getMovieDetails(1, null, null)));
    }

    @Test
    public void error401IsReportedAsHttpException() {
        enqueueFixture("error_401.json", 401);

        HttpException error = assertThrows(HttpException.class, () -> TmdbCalls.execute(api.getMovieGenres()));

        assertEquals(401, error.code());
    }

    @Test
    public void error404IsReportedAsHttpException() {
        enqueueFixture("error_404.json", 404);

        HttpException error = assertThrows(HttpException.class,
                () -> TmdbCalls.execute(api.getMovieDetails(999_999_999, null, null)));

        assertEquals(404, error.code());
    }

    @Test
    public void error429IsRetriedAfterRetryAfter() throws Exception {
        server.enqueue(new MockResponse.Builder().code(429).addHeader("Retry-After", "2").build());
        enqueueFixture("genre_movie_list.json");

        List<GenreDto> genres = Objects.requireNonNull(TmdbCalls.execute(api.getMovieGenres()).genres());

        assertEquals(19, genres.size());
        assertEquals(2, server.getRequestCount());
        assertEquals(List.of(2_000L), sleeps);
    }

    @Test
    public void error429RetriesUseGrowingPausesAndAreLimited() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse.Builder().code(429).build());
        }

        HttpException error = assertThrows(HttpException.class, () -> TmdbCalls.execute(api.getMovieGenres()));

        assertEquals(429, error.code());
        assertEquals(4, server.getRequestCount());
        assertEquals(List.of(1_000L, 2_000L, 4_000L), sleeps);
    }

    @Test
    public void noConnectionIsReportedAsIoException() {
        server.close();

        assertThrows(IOException.class, () -> TmdbCalls.execute(api.getMovieGenres()));
    }
}

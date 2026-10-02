package ru.kinopolka.core.data.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.TestData.movieDto;
import static ru.kinopolka.testing.TestData.pageOf;
import static ru.kinopolka.testing.TestData.tvDto;

import androidx.paging.PagingSource;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.testing.FakeTmdbApi;
import ru.kinopolka.testing.TestData;

public class PagingSourcesTest {

    private final FakeTmdbApi api = new FakeTmdbApi();
    private final Genre action = TestData.ACTION;
    private final Genre horror = TestData.HORROR;

    private static PagingSource.LoadResult.Page<Integer, Media> page(PagingSource.LoadResult<Integer, Media> result) {
        return (PagingSource.LoadResult.Page<Integer, Media>) result;
    }

    private SearchPagingSource search(String query, MediaFilter filter) {
        return new SearchPagingSource(api, query, filter, MoreExecutors.newDirectExecutorService());
    }

    private GenrePagingSource genre(Genre value, MediaFilter filter, MediaSort sort) {
        return new GenrePagingSource(api, value, filter, sort, MoreExecutors.newDirectExecutorService());
    }

    private static List<MediaKey> keys(PagingSource.LoadResult.Page<Integer, Media> page) {
        return page.getData().stream().map(Media::key).toList();
    }

    @Test
    public void searchWithoutFilterDropsPeople() throws Exception {
        PagingSource.LoadResult.Page<Integer, Media> page = page(search("тяжкие", MediaFilter.ALL)
                .loadFuture(new PagingSource.LoadParams.Refresh<>(null, 20, false)).get());

        assertEquals(List.of(new MediaKey(MediaType.TV, 1396), new MediaKey(MediaType.MOVIE, 559969)), keys(page));
        assertNull("single page", page.getNextKey());
        assertNull(page.getPrevKey());
        assertEquals(List.of("search/multi:тяжкие:1"), api.requests);
    }

    @Test
    public void searchWithAFilterUsesTheEndpointOfTheType() {
        api.search = (endpoint, query, number) -> pageOf(List.of(movieDto(number * 10)), number, 3);

        SearchPagingSource source = search("матрица", MediaFilter.MOVIES);
        PagingSource.LoadResult.Page<Integer, Media> first = page(source.load(1));
        PagingSource.LoadResult.Page<Integer, Media> second = page(source.load(2));

        assertEquals(List.of(new MediaKey(MediaType.MOVIE, 10)), keys(first));
        assertEquals(Integer.valueOf(2), first.getNextKey());
        assertEquals(Integer.valueOf(3), second.getNextKey());
        assertEquals(List.of("search/movie:матрица:1", "search/movie:матрица:2"), api.requests);
    }

    @Test
    public void seriesFilterUsesSearchTv() {
        api.search = (endpoint, query, number) -> pageOf(List.of(tvDto(5)));

        PagingSource.LoadResult.Page<Integer, Media> page = page(search("дюна", MediaFilter.SERIES).load(1));

        assertEquals(List.of(new MediaKey(MediaType.TV, 5)), keys(page));
        assertEquals(List.of("search/tv:дюна:1"), api.requests);
    }

    @Test
    public void networkErrorsBecomeAnErrorResult() {
        api.failure = new IOException("offline");

        PagingSource.LoadResult<Integer, Media> result = search("дюна", MediaFilter.SERIES).load(1);

        assertTrue(result instanceof PagingSource.LoadResult.Error);
    }

    @Test
    public void httpErrorsBecomeAnErrorResult() {
        api.failure = FakeTmdbApi.httpError(500);

        assertTrue(genre(action, MediaFilter.ALL, MediaSort.POPULARITY).load(1)
                instanceof PagingSource.LoadResult.Error);
    }

    @Test
    public void genrePageMergesMoviesAndSeriesByTheSelectedSort() {
        api.discoverMoviesPage = (genreId, sortBy, number) -> pageOf(List.of(
                movieDto(1, 1.0, 9.0, "2020-01-01", null), movieDto(2, 1.0, 6.0, "2020-01-01", null)), number, 2);
        api.discoverTvPage = (genreId, sortBy, number) -> pageOf(List.of(tvDto(1, 1.0, 7.5, "2020-01-01", null)),
                number, 1);

        PagingSource.LoadResult.Page<Integer, Media> page = page(genre(action, MediaFilter.ALL, MediaSort.RATING)
                .load(1));

        assertEquals(List.of(new MediaKey(MediaType.MOVIE, 1), new MediaKey(MediaType.TV, 1),
                new MediaKey(MediaType.MOVIE, 2)), keys(page));
        assertEquals("movies have one more page", Integer.valueOf(2), page.getNextKey());
        assertTrue(api.requests.contains("discover/movie:28:vote_average.desc:1"));
        assertTrue(api.requests.contains("discover/tv:10759:vote_average.desc:1"));
    }

    @Test
    public void genreWithoutSeriesRequestsOnlyMovies() {
        genre(horror, MediaFilter.ALL, MediaSort.NEWEST).load(1);

        assertEquals(List.of("discover/movie:27:primary_release_date.desc:1"), api.requests);
    }

    @Test
    public void titlesRepeatedOnTheNextPageAreDropped() {
        api.discoverMoviesPage = (genreId, sortBy, number) -> pageOf(
                (number == 1 ? List.of(1, 2) : List.of(2, 3)).stream().map(id -> movieDto(id)).toList(), number, 2);
        GenrePagingSource source = genre(action, MediaFilter.MOVIES, MediaSort.POPULARITY);

        source.load(1);
        PagingSource.LoadResult.Page<Integer, Media> second = page(source.load(2));

        assertEquals(List.of(3), second.getData().stream().map(Media::tmdbId).toList());
        assertNull(second.getNextKey());
    }

    @Test
    public void refreshAlwaysStartsFromTheFirstPage() {
        assertNull(genre(action, MediaFilter.ALL, MediaSort.POPULARITY).getRefreshKey(
                new androidx.paging.PagingState<>(List.of(), null,
                        new androidx.paging.PagingConfig(20), 0)));
    }
}

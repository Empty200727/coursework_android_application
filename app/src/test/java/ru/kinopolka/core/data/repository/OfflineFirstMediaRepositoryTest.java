package ru.kinopolka.core.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.database.DatabaseTest;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.ui.PosterAdapter;
import ru.kinopolka.testing.FakeTmdbApi;
import ru.kinopolka.testing.PagingTesting;
import ru.kinopolka.testing.TestData;
import ru.kinopolka.testing.TestTimeProvider;

/** Shelves are cached in {@code feed_item} for 6 hours (N-03), checked with a controllable clock. */
@RunWith(AndroidJUnit4.class)
public class OfflineFirstMediaRepositoryTest extends DatabaseTest {

    private final FakeTmdbApi api = new FakeTmdbApi();
    private final TestTimeProvider clock = new TestTimeProvider();

    private OfflineFirstMediaRepository repository() {
        return new OfflineFirstMediaRepository(api, database, database.mediaDao(), database.feedDao(), clock,
                AppExecutors.direct());
    }

    private static List<MediaKey> keys(List<Media> media) {
        return media.stream().map(Media::key).toList();
    }

    @Test
    public void shelfIsEmptyUntilLoadedAndThenReadFromTheDatabase() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.Trending(MediaFilter.ALL);
        assertTrue(awaitValue(repository.observeShelf(shelf)).isEmpty());

        assertEquals(RefreshResult.UPDATED, repository.refreshShelf(shelf, false));

        assertEquals(List.of(new MediaKey(MediaType.MOVIE, 693134), new MediaKey(MediaType.TV, 94997)),
                keys(awaitValue(repository.observeShelf(shelf), list -> !list.isEmpty())));
        assertEquals(List.of("trending/all/week"), api.requests);
    }

    @Test
    public void trendingOfOneTypeUsesItsEndpoint() {
        OfflineFirstMediaRepository repository = repository();

        repository.refreshShelf(new Shelf.Trending(MediaFilter.MOVIES), false);
        repository.refreshShelf(new Shelf.Trending(MediaFilter.SERIES), false);

        assertEquals(List.of("trending/movie/week", "trending/tv/week"), api.requests);
    }

    @Test
    public void freshShelfIsNotReloadedWithin6Hours() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.Popular(MediaType.MOVIE);
        repository.refreshShelf(shelf, false);

        clock.advanceBy(TimeUnit.HOURS.toMillis(6) - TimeUnit.MINUTES.toMillis(1));
        assertEquals(RefreshResult.SKIPPED, repository.refreshShelf(shelf, false));
        assertEquals(1, api.requests.size());

        clock.advanceBy(TimeUnit.MINUTES.toMillis(1));
        assertEquals(RefreshResult.UPDATED, repository.refreshShelf(shelf, false));
        assertEquals(2, api.requests.size());
    }

    @Test
    public void forcedRefreshIgnoresTheCacheAge() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.Popular(MediaType.TV);
        repository.refreshShelf(shelf, false);

        assertEquals(RefreshResult.UPDATED, repository.refreshShelf(shelf, true));
        assertEquals(List.of("tv/popular", "tv/popular"), api.requests);
    }

    @Test
    public void failedRefreshKeepsTheCachedShelf() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.Trending(MediaFilter.ALL);
        repository.refreshShelf(shelf, false);
        clock.advanceBy(TimeUnit.HOURS.toMillis(7));
        api.failure = new IOException("offline");

        assertEquals(new RefreshResult.Failed(DataError.NO_CONNECTION), repository.refreshShelf(shelf, false));
        assertEquals(2, awaitValue(repository.observeShelf(shelf)).size());
    }

    @Test
    public void genreShelfForAllMergesMoviesAndSeriesByPopularity() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.ByGenre(TestData.ACTION, MediaFilter.ALL);

        repository.refreshShelf(shelf, false);

        // discover_movie: «Матрица» 85.3; discover_tv: «Дом Дракона» 980.2, «Пацаны» 40.5.
        assertEquals(List.of(new MediaKey(MediaType.TV, 94997), new MediaKey(MediaType.MOVIE, 603),
                        new MediaKey(MediaType.TV, 76479)),
                keys(awaitValue(repository.observeShelf(shelf), list -> list.size() == 3)));
        assertEquals(Set.of("discover/movie:28:popularity.desc:1", "discover/tv:10759:popularity.desc:1"),
                new HashSet<>(api.requests));
    }

    @Test
    public void filteredGenreShelfUsesOneTypeAndSharesTheCacheWithAll() {
        OfflineFirstMediaRepository repository = repository();
        repository.refreshShelf(new Shelf.ByGenre(TestData.ACTION, MediaFilter.ALL), false);

        Shelf series = new Shelf.ByGenre(TestData.ACTION, MediaFilter.SERIES);
        assertEquals(RefreshResult.SKIPPED, repository.refreshShelf(series, false));
        assertEquals(List.of(94997, 76479), awaitValue(repository.observeShelf(series)).stream()
                .map(Media::tmdbId).toList());
    }

    @Test
    public void genreWithoutSeriesShowsOnlyMovies() {
        OfflineFirstMediaRepository repository = repository();
        Shelf shelf = new Shelf.ByGenre(TestData.HORROR, MediaFilter.ALL);

        repository.refreshShelf(shelf, false);

        assertEquals(List.of("discover/movie:27:popularity.desc:1"), api.requests);
        assertEquals(List.of(603), awaitValue(repository.observeShelf(shelf)).stream().map(Media::tmdbId).toList());
        assertTrue("no series shelf", awaitValue(repository.observeShelf(
                new Shelf.ByGenre(TestData.HORROR, MediaFilter.SERIES))).isEmpty());
    }

    @Test
    public void searchResultsComeFromTheNetworkWithoutPeople() {
        List<Media> results = PagingTesting.firstPage(repository().search("тяжкие", MediaFilter.ALL),
                PosterAdapter.DIFF);

        assertEquals(List.of(new MediaKey(MediaType.TV, 1396), new MediaKey(MediaType.MOVIE, 559969)), keys(results));
    }

    @Test
    public void genreListIsPagedFromDiscover() {
        List<Media> results = PagingTesting.firstPage(
                repository().genreMedia(TestData.HORROR, MediaFilter.ALL, MediaSort.POPULARITY), PosterAdapter.DIFF);

        assertEquals(List.of(603), results.stream().map(Media::tmdbId).toList());
    }
}

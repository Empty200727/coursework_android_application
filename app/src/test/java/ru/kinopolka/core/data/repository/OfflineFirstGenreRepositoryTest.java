package ru.kinopolka.core.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.GenreCatalog;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.database.DatabaseTest;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.testing.FakeTmdbApi;
import ru.kinopolka.testing.TestTimeProvider;

@RunWith(AndroidJUnit4.class)
public class OfflineFirstGenreRepositoryTest extends DatabaseTest {

    private final FakeTmdbApi api = new FakeTmdbApi();
    private final TestTimeProvider clock = new TestTimeProvider();

    private OfflineFirstGenreRepository repository() {
        return new OfflineFirstGenreRepository(api, database.genreDao(), clock);
    }

    private static List<String> keys(List<Genre> genres) {
        return genres.stream().map(Genre::key).toList();
    }

    @Test
    public void emptyCacheIsLoadedFromBothGenreEndpoints() {
        OfflineFirstGenreRepository repository = repository();
        assertTrue(awaitValue(repository.observeGenres()).isEmpty());

        assertEquals(RefreshResult.UPDATED, repository.refresh(false));

        assertEquals(2, api.genreRequests());
        List<Genre> genres = awaitValue(repository.observeGenres(), list -> !list.isEmpty());
        assertEquals("Боевик", genres.stream().filter(it -> it.key().equals("action")).findFirst().orElseThrow()
                .name());
        assertEquals(Long.valueOf(clock.nowMillis()), database.genreDao().oldestCachedAt(MediaType.MOVIE));
        assertEquals(Long.valueOf(clock.nowMillis()), database.genreDao().oldestCachedAt(MediaType.TV));
    }

    @Test
    public void observersReceiveTheFullCatalogOnceItIsLoaded() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);

        List<String> expected = GenreCatalog.MAPPINGS.stream().map(GenreCatalog.Mapping::key).toList();
        assertEquals(expected, keys(awaitValue(repository.observeGenres(), list -> list.size() == expected.size())));
    }

    @Test
    public void freshCacheIsNotReloaded() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);
        clock.advanceBy(TimeUnit.DAYS.toMillis(6));

        assertEquals(RefreshResult.SKIPPED, repository.refresh(false));
        assertEquals(2, api.genreRequests());
    }

    @Test
    public void cacheOlderThan7DaysIsReloaded() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);
        clock.advanceBy(TimeUnit.DAYS.toMillis(7));

        assertEquals(RefreshResult.UPDATED, repository.refresh(false));
        assertEquals(4, api.genreRequests());
        assertEquals(Long.valueOf(clock.nowMillis()), database.genreDao().oldestCachedAt(MediaType.MOVIE));
    }

    @Test
    public void forcedRefreshIgnoresTheCacheAge() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);

        assertEquals(RefreshResult.UPDATED, repository.refresh(true));
        assertEquals(4, api.genreRequests());
    }

    @Test
    public void missingGenresOfOneTypeMakeTheCacheStale() {
        OfflineFirstGenreRepository repository = repository();
        GenreListResponseDto tvGenres = api.tvGenres;
        api.tvGenres = new GenreListResponseDto(List.of());
        repository.refresh(false);
        api.tvGenres = tvGenres;

        assertEquals(RefreshResult.UPDATED, repository.refresh(false));
        assertEquals(Integer.valueOf(10762), repository.getGenre("kids").tvGenreId());
    }

    @Test
    public void failedRefreshKeepsCachedGenres() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);
        clock.advanceBy(TimeUnit.DAYS.toMillis(8));
        api.failure = new IOException("offline");

        assertEquals(new RefreshResult.Failed(DataError.NO_CONNECTION), repository.refresh(false));
        assertEquals(GenreCatalog.MAPPINGS.size(), awaitValue(repository.observeGenres()).size());
    }

    @Test
    public void serverErrorsAreClassified() {
        OfflineFirstGenreRepository repository = repository();
        api.failure = FakeTmdbApi.httpError(401);
        assertEquals(new RefreshResult.Failed(DataError.UNAUTHORIZED), repository.refresh(false));

        api.failure = FakeTmdbApi.httpError(503);
        assertEquals(new RefreshResult.Failed(DataError.SERVER), repository.refresh(false));

        api.failure = null;
        api.movieGenres = null;
        assertEquals("no body", new RefreshResult.Failed(DataError.BAD_RESPONSE), repository.refresh(false));

        assertEquals(DataError.BAD_RESPONSE, DataError.of(new JsonParseException("broken json")));
        assertEquals(DataError.SERVER, DataError.of(new IllegalStateException()));
    }

    @Test
    public void emptyAnswerDoesNotWipeTheCatalog() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);
        clock.advanceBy(TimeUnit.DAYS.toMillis(8));
        api.movieGenres = new GenreListResponseDto(null);
        api.tvGenres = new GenreListResponseDto(List.of());

        repository.refresh(false);

        assertEquals(GenreCatalog.MAPPINGS.size(), awaitValue(repository.observeGenres()).size());
    }

    @Test
    public void genresAreFilteredByMediaType() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);

        List<String> series = keys(awaitValue(repository.observeGenres(MediaFilter.SERIES)));
        List<String> movies = keys(awaitValue(repository.observeGenres(MediaFilter.MOVIES)));

        assertTrue(!series.contains("horror") && series.contains("kids"));
        assertTrue(!movies.contains("kids") && movies.contains("horror"));
    }

    @Test
    public void genreIsFoundByKeyAndNamesAreGroupedByType() {
        OfflineFirstGenreRepository repository = repository();
        repository.refresh(false);

        assertEquals(Integer.valueOf(27), repository.getGenre("horror").movieGenreId());
        assertNull(repository.getGenre("unknown"));
        Map<MediaType, Map<Integer, String>> names = awaitValue(repository.observeGenreNames());
        assertEquals("Боевик", names.get(MediaType.MOVIE).get(28));
        assertEquals("Боевик и Приключения", names.get(MediaType.TV).get(10759));
    }
}

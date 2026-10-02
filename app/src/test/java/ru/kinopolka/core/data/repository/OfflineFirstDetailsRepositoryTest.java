package ru.kinopolka.core.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.mapper.EntityMappers;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.database.DatabaseTest;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.TvDetailsDto;
import ru.kinopolka.testing.FakeTmdbApi;
import ru.kinopolka.testing.Fixtures;
import ru.kinopolka.testing.TestTimeProvider;

/** Title card: one request, cache of 24 hours (N-03), similar as a fallback (F-10), English overview (N-07). */
@RunWith(AndroidJUnit4.class)
public class OfflineFirstDetailsRepositoryTest extends DatabaseTest {

    private final FakeTmdbApi api = new FakeTmdbApi();
    private final TestTimeProvider clock = new TestTimeProvider();
    private final MediaKey fightClub = new MediaKey(MediaType.MOVIE, 550);
    private final MediaKey breakingBad = new MediaKey(MediaType.TV, 1396);

    private OfflineFirstDetailsRepository repository() {
        return new OfflineFirstDetailsRepository(api, database, database.mediaDao(), database.castDao(),
                database.relatedMediaDao(), database.genreDao(), clock);
    }

    private MediaDetails details(OfflineFirstDetailsRepository repository, MediaKey key) {
        return awaitValue(repository.observeDetails(key), Objects::nonNull);
    }

    @Before
    public void cacheGenres() {
        List<TmdbGenre> genres = new ArrayList<>(NetworkMappers.toTmdbGenres(
                Fixtures.parse("genre_movie_list.json", GenreListResponseDto.class), MediaType.MOVIE));
        genres.addAll(NetworkMappers.toTmdbGenres(
                Fixtures.parse("genre_tv_list.json", GenreListResponseDto.class), MediaType.TV));
        List<GenreEntity> entities = genres.stream().map(it -> EntityMappers.toEntity(it, 0L)).toList();
        database.genreDao().upsert(entities);
    }

    @Test
    public void movieCardIsLoadedInOneRequestAndReadFromTheDatabase() {
        OfflineFirstDetailsRepository repository = repository();
        assertNull(awaitValue(repository.observeDetails(fightClub)));

        assertEquals(RefreshResult.UPDATED, repository.refreshDetails(fightClub, false));

        assertEquals(List.of("movie/550:credits,recommendations,similar:"), api.requests);
        MediaDetails details = details(repository, fightClub);
        assertEquals("Бойцовский клуб", details.media().title());
        assertEquals(Integer.valueOf(139), details.runtimeMinutes());
        assertEquals(List.of("Драма", "Триллер"), details.genres().stream().map(TmdbGenre::name).toList());
        assertEquals(10, details.cast().size());
        assertEquals("Эдвард Нортон", details.cast().get(0).name());
        assertEquals("recommendations come first", List.of(807, 680),
                details.related().stream().map(Media::tmdbId).toList());
        assertFalse(details.overviewFallback());
        assertTrue(details.complete());
    }

    @Test
    public void similarTitlesAreShownWhenThereAreNoRecommendations() {
        api.movieDetails = (id, language) -> Fixtures.parse("movie_details_550.json", MovieDetailsDto.class)
                .withRecommendations(new PagedResponseDto<>(null, List.of(), null, null));
        OfflineFirstDetailsRepository repository = repository();

        repository.refreshDetails(fightClub, false);

        MediaDetails details = details(repository, fightClub);
        assertTrue(details.recommendations().isEmpty());
        assertEquals(List.of(new MediaKey(MediaType.MOVIE, 1954)),
                details.related().stream().map(Media::key).toList());
    }

    @Test
    public void missingRussianOverviewIsReplacedByTheEnglishOne() {
        api.tvDetails = (id, language) -> {
            TvDetailsDto dto = Fixtures.parse("tv_details_1396.json", TvDetailsDto.class);
            return "en-US".equals(language) ? dto.withOverview("A chemistry teacher turns to crime.") : dto;
        };
        OfflineFirstDetailsRepository repository = repository();

        repository.refreshDetails(breakingBad, false);

        assertEquals(List.of("tv/1396:aggregate_credits,recommendations,similar:", "tv/1396::en-US"), api.requests);
        MediaDetails details = details(repository, breakingBad);
        assertEquals("A chemistry teacher turns to crime.", details.media().overview());
        assertTrue(details.overviewFallback());
        assertEquals(Integer.valueOf(5), details.numberOfSeasons());
        assertEquals("Джесси Пинкман", details.cast().stream().filter(it -> it.personId() == 84497)
                .findFirst().orElseThrow().character());
    }

    @Test
    public void failedEnglishFallbackKeepsTheCardWithoutOverview() {
        api.tvDetails = (id, language) -> {
            if ("en-US".equals(language)) {
                api.failure = new IOException("offline");
            }
            return Fixtures.parse("tv_details_1396.json", TvDetailsDto.class);
        };
        OfflineFirstDetailsRepository repository = repository();

        assertEquals(RefreshResult.UPDATED, repository.refreshDetails(breakingBad, false));

        MediaDetails details = details(repository, breakingBad);
        assertNull(details.media().overview());
        assertFalse(details.overviewFallback());
    }

    @Test
    public void cardIsCachedFor24Hours() {
        OfflineFirstDetailsRepository repository = repository();
        repository.refreshDetails(fightClub, false);

        clock.advanceBy(TimeUnit.HOURS.toMillis(23));
        assertEquals(RefreshResult.SKIPPED, repository.refreshDetails(fightClub, false));

        clock.advanceBy(TimeUnit.HOURS.toMillis(1));
        assertEquals(RefreshResult.UPDATED, repository.refreshDetails(fightClub, false));
        assertEquals(2, api.requests.size());
    }

    @Test
    public void offlineTheCachedCardStaysAvailable() {
        OfflineFirstDetailsRepository repository = repository();
        repository.refreshDetails(fightClub, false);
        clock.advanceBy(TimeUnit.HOURS.toMillis(25));
        api.failure = new IOException("offline");

        assertEquals(new RefreshResult.Failed(DataError.NO_CONNECTION), repository.refreshDetails(fightClub, false));
        assertEquals(10, details(repository, fightClub).cast().size());
    }

    @Test
    public void titleKnownFromAListIsShownBeforeItsCardIsLoaded() {
        database.mediaDao().upsertSummaries(List.of(media(550)));

        MediaDetails details = details(repository(), fightClub);

        assertFalse(details.complete());
        assertTrue(details.cast().isEmpty());
    }

    @Test
    public void unknownTitleIsReported() {
        api.failure = FakeTmdbApi.httpError(404);

        assertEquals(new RefreshResult.Failed(DataError.NOT_FOUND), repository().refreshDetails(fightClub, false));
    }

    @Test
    public void answerWithoutIdIsABadResponse() {
        api.movieDetails = (id, language) -> new MovieDetailsDto(null, "Без id", null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        assertEquals(new RefreshResult.Failed(DataError.BAD_RESPONSE), repository().refreshDetails(fightClub, false));
    }
}

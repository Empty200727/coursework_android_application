package ru.kinopolka.core.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import ru.kinopolka.core.database.entity.FeedItemEntity;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.database.entity.MediaGenreEntity;
import ru.kinopolka.core.database.entity.RelatedMediaEntity;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.TvDetailsDto;
import ru.kinopolka.testing.Fixtures;

public class EntityMappersTest {

    private final Media media = new Media(new MediaKey(MediaType.TV, 1396), "Во все тяжкие", "Breaking Bad",
            "Учитель химии", "/poster.jpg", null, LocalDate.of(2008, 1, 20), 8.9, 15432, 312.5, List.of(18, 80, 18));

    @Test
    public void mediaSurvivesTheRoundTripThroughTheEntity() {
        MediaEntity entity = EntityMappers.toEntity(media, 42L);

        assertEquals(MediaType.TV, entity.mediaType);
        assertEquals("2008-01-20", entity.releaseDate);
        assertEquals(42L, entity.cachedAt);
        assertNull(entity.detailsCachedAt);
        assertEquals(media.withGenreIds(List.of(18, 80)), EntityMappers.toMedia(entity, List.of(18, 80)));
    }

    @Test
    public void genreLinksAreUnique() {
        List<MediaGenreEntity> genres = EntityMappers.toGenreEntities(media);
        assertEquals(List.of(18, 80), genres.stream().map(it -> it.genreId).toList());
        assertTrue(genres.stream().allMatch(it -> it.mediaType == MediaType.TV && it.tmdbId == 1396));
    }

    @Test
    public void movieDetailsFillTheDetailColumns() {
        MediaDetails details = Objects.requireNonNull(NetworkMappers.toMediaDetails(
                Fixtures.parse("movie_details_550.json", MovieDetailsDto.class)));

        MediaEntity entity = EntityMappers.toEntity(details, 100L);

        assertEquals(Integer.valueOf(139), entity.runtime);
        assertNull(entity.numberOfSeasons);
        assertEquals(Long.valueOf(100L), entity.detailsCachedAt);
        assertFalse(entity.isOverviewFallback);
    }

    @Test
    public void tvDetailsFillSeasonsAndLastAirDate() {
        MediaDetails details = Objects.requireNonNull(NetworkMappers.toMediaDetails(
                Fixtures.parse("tv_details_1396.json", TvDetailsDto.class)));

        MediaEntity entity = EntityMappers.toEntity(details, 100L);

        assertEquals(Integer.valueOf(5), entity.numberOfSeasons);
        assertEquals("2013-09-29", entity.lastAirDate);
        assertEquals(Boolean.FALSE, entity.inProduction);
        assertNull(entity.runtime);
    }

    @Test
    public void castGenresAndLibraryEntriesSurviveTheRoundTrip() {
        CastMember cast = new CastMember(17419, "Брайан Крэнстон", "Уолтер Уайт", null, 0);
        assertEquals(cast, EntityMappers.toCastMember(EntityMappers.toEntity(cast, media.key())));

        TmdbGenre genre = new TmdbGenre(MediaType.TV, 10765, "НФ и Фэнтези");
        assertEquals(genre, EntityMappers.toTmdbGenre(EntityMappers.toEntity(genre, 1L)));

        LibraryEntry entry = new LibraryEntry(media.key(), WatchStatus.WATCHED, true, Instant.ofEpochMilli(1_000),
                Instant.ofEpochMilli(2_000), 9, "/data/posters/tv_1396.jpg");
        assertEquals(entry, EntityMappers.toLibraryEntry(EntityMappers.toEntity(entry)));
    }

    @Test
    public void relatedAndFeedItemsKeepOrderAndDropDuplicates() {
        Media other = new Media(new MediaKey(MediaType.MOVIE, 1396), media.title(), null, null, null, null, null,
                0.0, 0, 0.0, List.of());

        List<RelatedMediaEntity> related = EntityMappers.relatedEntities(new MediaKey(MediaType.MOVIE, 550),
                RelatedKind.SIMILAR, List.of(media, other, media));
        assertEquals(List.of(0, 1), related.stream().map(it -> it.position).toList());
        assertEquals(List.of(MediaType.TV, MediaType.MOVIE), related.stream().map(it -> it.targetMediaType).toList());

        List<FeedItemEntity> feed = EntityMappers.feedItemEntities("trending", List.of(other, media, other), 5L);
        assertEquals(List.of(MediaType.MOVIE, MediaType.TV), feed.stream().map(it -> it.mediaType).toList());
        assertTrue(feed.stream().allMatch(it -> it.feedKey.equals("trending") && it.fetchedAt == 5L));
    }

    @Test
    public void libraryEntryWithoutStatusAndFavoriteIsEmpty() {
        LibraryEntry entry = new LibraryEntry(media.key(), WatchStatus.NONE, false, Instant.EPOCH, null, null, null);
        assertTrue(entry.isEmpty());
        assertFalse(entry.withFavorite(true).isEmpty());
        assertFalse(entry.withStatus(WatchStatus.WANT, Instant.EPOCH, null).isEmpty());
    }

    @Test
    public void cachedCardKeepsOnlyFieldsOfItsType() {
        MediaEntity entity = new MediaEntity(MediaType.TV, 1, "Сериал", null, null, true, null, null, "2019-01-01",
                "2021-05-01", 8.0, 10, 1.0, 50, 3, true, 1L, null);

        MediaDetails details = EntityMappers.toMediaDetails(entity, List.of(18, 99),
                java.util.Map.of(18, "Драма"), List.of(), List.of(), List.of());

        assertNull("runtime of a series is ignored", details.runtimeMinutes());
        assertEquals(Integer.valueOf(3), details.numberOfSeasons());
        assertEquals(LocalDate.of(2021, 5, 1), details.lastAirDate());
        assertEquals(List.of(new TmdbGenre(MediaType.TV, 18, "Драма")), details.genres());
        assertTrue(details.overviewFallback());
        assertFalse("details were never loaded", details.complete());
    }
}

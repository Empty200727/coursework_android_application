package ru.kinopolka.core.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.junit.Test;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.model.GenreDto;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.TvDetailsDto;
import ru.kinopolka.testing.Fixtures;

public class NetworkMappersTest {

    private static MediaListItemDto item(Integer id, String mediaType, String title) {
        return new MediaListItemDto(id, mediaType, title, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    @Test
    public void searchMultiDropsPeopleAndMapsMoviesAndSeries() {
        List<Media> media = NetworkMappers.toMediaList(Fixtures.page("search_multi.json"));

        assertEquals(List.of(new MediaKey(MediaType.TV, 1396), new MediaKey(MediaType.MOVIE, 559969)),
                media.stream().map(Media::key).toList());
        Media series = media.get(0);
        assertEquals("Во все тяжкие", series.title());
        assertEquals("Breaking Bad", series.originalTitle());
        assertEquals(LocalDate.of(2008, 1, 20), series.releaseDate());
        assertEquals(Integer.valueOf(2008), series.releaseYear());
        assertEquals(8.9, series.voteAverage(), 0.0);
        assertEquals(15432, series.voteCount());
        assertEquals(List.of(18, 80), series.genreIds());
        assertEquals("/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg", series.posterPath());
    }

    @Test
    public void emptyOverviewBecomesNull() {
        Media movie = NetworkMappers.toMediaList(Fixtures.page("search_multi.json")).stream()
                .filter(it -> it.mediaType() == MediaType.MOVIE).findFirst().orElseThrow();

        assertNull(movie.overview());
        assertEquals(Integer.valueOf(2019), movie.releaseYear());
    }

    @Test
    public void listWithoutMediaTypeUsesTheDefaultType() {
        PagedResponseDto<MediaListItemDto> movies = Fixtures.page("movie_popular.json");
        PagedResponseDto<MediaListItemDto> series = Fixtures.page("tv_popular.json");

        assertEquals(MediaType.MOVIE, NetworkMappers.toMediaList(movies, MediaType.MOVIE).get(0).mediaType());
        List<Media> tv = NetworkMappers.toMediaList(series, MediaType.TV);
        assertEquals(1, tv.size());
        assertEquals(MediaType.TV, tv.get(0).mediaType());
        assertTrue(NetworkMappers.toMediaList(movies, null).isEmpty());
        assertTrue(NetworkMappers.toMediaList(null).isEmpty());
    }

    @Test
    public void missingTitleFallsBackToOriginalTitleAndEmptyDateToNull() {
        Media untitled = NetworkMappers.toMediaList(Fixtures.page("movie_popular.json"), MediaType.MOVIE).stream()
                .filter(it -> it.tmdbId() == 1234567).findFirst().orElseThrow();

        assertEquals("Без даты", untitled.title());
        assertNull(untitled.releaseDate());
        assertNull(untitled.releaseYear());
        assertNull(untitled.posterPath());
        assertEquals(0.0, untitled.voteAverage(), 0.0);
    }

    @Test
    public void itemsWithoutIdOrAnyTitleAreSkipped() {
        assertNull(NetworkMappers.toMediaOrNull(item(null, "movie", "Без id")));
        assertNull(NetworkMappers.toMediaOrNull(item(1, "movie", " ")));
        assertNull(NetworkMappers.toMediaOrNull(item(1, "collection", "Коллекция")));
    }

    @Test
    public void yearIsParsedFromTheDate() {
        assertEquals(LocalDate.of(1999, 10, 15), NetworkMappers.toLocalDateOrNull("1999-10-15"));
        assertNull(NetworkMappers.toLocalDateOrNull(""));
        assertNull(NetworkMappers.toLocalDateOrNull("1999"));
        assertNull(NetworkMappers.toLocalDateOrNull(null));
    }

    @Test
    public void genreListMapsNamesAndSkipsBrokenEntries() {
        List<TmdbGenre> genres = NetworkMappers.toTmdbGenres(
                Fixtures.parse("genre_movie_list.json", GenreListResponseDto.class), MediaType.MOVIE);

        assertEquals(19, genres.size());
        assertTrue(genres.stream().allMatch(it -> it.mediaType() == MediaType.MOVIE));
        assertEquals(List.of(), NetworkMappers.toTmdbGenres(
                new GenreListResponseDto(List.of(new GenreDto(1, ""))), MediaType.TV));
        assertEquals(List.of(), NetworkMappers.toTmdbGenres(new GenreListResponseDto(null), MediaType.TV));
    }

    @Test
    public void movieDetailsMapRuntimeGenresTop10CastAndRelatedTitles() {
        MediaDetails details = Objects.requireNonNull(NetworkMappers.toMediaDetails(
                Fixtures.parse("movie_details_550.json", MovieDetailsDto.class)));

        assertEquals(new MediaKey(MediaType.MOVIE, 550), details.media().key());
        assertEquals("Бойцовский клуб", details.media().title());
        assertEquals("Fight Club", details.media().originalTitle());
        assertEquals(Integer.valueOf(139), details.runtimeMinutes());
        assertNull(details.numberOfSeasons());
        assertEquals(List.of("драма", "триллер"), details.genres().stream().map(TmdbGenre::name).toList());
        assertEquals(List.of(18, 53), details.media().genreIds());
        assertFalse(details.overviewFallback());

        assertEquals(NetworkMappers.MAX_CAST_MEMBERS, details.cast().size());
        assertEquals("Эдвард Нортон", details.cast().get(0).name());
        assertEquals("Рассказчик", details.cast().get(0).character());
        assertEquals("Тайлер Дёрден", details.cast().stream().filter(it -> it.personId() == 287)
                .findFirst().orElseThrow().character());
        assertEquals(IntStream.range(0, 10).boxed().toList(),
                details.cast().stream().map(CastMember::order).toList());

        assertEquals(List.of(807, 680), details.recommendations().stream().map(Media::tmdbId).toList());
        assertEquals(List.of(new MediaKey(MediaType.MOVIE, 1954)),
                details.similar().stream().map(Media::key).toList());
    }

    @Test
    public void tvDetailsMapSeasonsAirDatesAndTheMainRole() {
        MediaDetails details = Objects.requireNonNull(NetworkMappers.toMediaDetails(
                Fixtures.parse("tv_details_1396.json", TvDetailsDto.class)));

        assertEquals(new MediaKey(MediaType.TV, 1396), details.media().key());
        assertEquals(Integer.valueOf(2008), details.media().releaseYear());
        assertEquals(LocalDate.of(2013, 9, 29), details.lastAirDate());
        assertEquals(Integer.valueOf(5), details.numberOfSeasons());
        assertEquals(Boolean.FALSE, details.inProduction());
        assertNull(details.runtimeMinutes());
        assertNull(details.media().overview());
        assertEquals("Джесси Пинкман", details.cast().stream().filter(it -> it.personId() == 84497)
                .findFirst().orElseThrow().character());
        assertEquals(List.of(new MediaKey(MediaType.TV, 60059), new MediaKey(MediaType.MOVIE, 559969)),
                details.recommendations().stream().map(Media::key).toList());
        assertTrue(details.similar().isEmpty());
    }

    @Test
    public void detailsWithoutIdAreRejected() {
        assertNull(NetworkMappers.toMediaDetails(new MovieDetailsDto(null, "Без id", null, null, null, null, null,
                null, null, null, null, null, null, null, null)));
        assertNull(NetworkMappers.toMediaDetails(new TvDetailsDto(null, "Без id", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null)));
    }
}

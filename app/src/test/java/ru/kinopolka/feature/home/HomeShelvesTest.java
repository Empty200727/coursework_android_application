package ru.kinopolka.feature.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ru.kinopolka.core.data.GenreCatalog;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.testing.Fixtures;

public class HomeShelvesTest {

    private final List<Genre> genres = catalog();

    private static List<Genre> catalog() {
        List<TmdbGenre> tmdb = new ArrayList<>(NetworkMappers.toTmdbGenres(
                Fixtures.parse("genre_movie_list.json", GenreListResponseDto.class), MediaType.MOVIE));
        tmdb.addAll(NetworkMappers.toTmdbGenres(
                Fixtures.parse("genre_tv_list.json", GenreListResponseDto.class), MediaType.TV));
        return GenreCatalog.merge(tmdb);
    }

    private static List<String> genreKeys(List<Shelf> shelves) {
        return shelves.stream().filter(Shelf.ByGenre.class::isInstance)
                .map(it -> ((Shelf.ByGenre) it).genre().key()).toList();
    }

    @Test
    public void allShowsTrendingBothPopularShelvesAnd8Genres() {
        List<Shelf> shelves = HomeShelves.homeShelves(MediaFilter.ALL, genres);

        assertEquals(List.of(new Shelf.Trending(MediaFilter.ALL), new Shelf.Popular(MediaType.MOVIE),
                new Shelf.Popular(MediaType.TV)), shelves.subList(0, 3));
        assertEquals(List.of("action", "comedy", "drama", "animation", "science_fiction", "crime", "horror", "family"),
                genreKeys(shelves));
        assertTrue(shelves.stream().filter(Shelf.ByGenre.class::isInstance)
                .allMatch(it -> ((Shelf.ByGenre) it).filter() == MediaFilter.ALL));
    }

    @Test
    public void seriesSkipPopularMoviesAndGenresWithoutSeries() {
        List<Shelf> shelves = HomeShelves.homeShelves(MediaFilter.SERIES, genres);

        assertEquals(List.of(new Shelf.Trending(MediaFilter.SERIES), new Shelf.Popular(MediaType.TV)),
                shelves.subList(0, 2));
        assertFalse(genreKeys(shelves).contains("horror"));
        assertEquals(HomeShelves.MAX_GENRE_SHELVES, genreKeys(shelves).size());
    }

    @Test
    public void moviesSkipPopularSeries() {
        List<Shelf> shelves = HomeShelves.homeShelves(MediaFilter.MOVIES, genres);

        assertFalse(shelves.contains(new Shelf.Popular(MediaType.TV)));
        assertTrue(shelves.contains(new Shelf.Popular(MediaType.MOVIE)));
    }

    @Test
    public void withoutGenresOnlyCollectionsAreShown() {
        assertEquals(3, HomeShelves.homeShelves(MediaFilter.ALL, List.of()).size());
    }

    @Test
    public void shelvesOfAnotherFilterDoNotMatch() {
        Genre comedy = genres.stream().filter(it -> it.key().equals("comedy")).findFirst().orElseThrow();

        assertFalse(HomeShelves.matches(new Shelf.Trending(MediaFilter.ALL), MediaFilter.SERIES));
        assertFalse(HomeShelves.matches(new Shelf.Popular(MediaType.MOVIE), MediaFilter.SERIES));
        assertTrue(HomeShelves.matches(new Shelf.Popular(MediaType.TV), MediaFilter.ALL));
        assertFalse(HomeShelves.matches(new Shelf.ByGenre(comedy, MediaFilter.ALL), MediaFilter.MOVIES));
        assertEquals("genre-comedy", new Shelf.ByGenre(comedy, MediaFilter.ALL).key());
    }
}

package ru.kinopolka.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.testing.Fixtures;

public class GenreCatalogTest {

    private final List<TmdbGenre> movieGenres = NetworkMappers.toTmdbGenres(
            Fixtures.parse("genre_movie_list.json", GenreListResponseDto.class), MediaType.MOVIE);
    private final List<TmdbGenre> tvGenres = NetworkMappers.toTmdbGenres(
            Fixtures.parse("genre_tv_list.json", GenreListResponseDto.class), MediaType.TV);
    private final List<Genre> catalog = GenreCatalog.merge(all());

    private List<TmdbGenre> all() {
        List<TmdbGenre> genres = new ArrayList<>(movieGenres);
        genres.addAll(tvGenres);
        return genres;
    }

    private Genre genre(String key) {
        return catalog.stream().filter(it -> it.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    public void genresWithEqualIdsAreMerged() {
        Genre comedy = genre("comedy");
        assertEquals(Integer.valueOf(35), comedy.movieGenreId());
        assertEquals(Integer.valueOf(35), comedy.tvGenreId());
        assertEquals("Комедия", comedy.name());
    }

    @Test
    public void genresWithDifferentIdsAreLinkedByTheTable() {
        assertEquals(Integer.valueOf(28), genre("action").movieGenreId());
        assertEquals(Integer.valueOf(10759), genre("action").tvGenreId());
        assertEquals(Integer.valueOf(14), genre("fantasy").movieGenreId());
        assertEquals(Integer.valueOf(10765), genre("fantasy").tvGenreId());
        assertEquals(Integer.valueOf(10752), genre("war").movieGenreId());
        assertEquals(Integer.valueOf(10768), genre("war").tvGenreId());
    }

    @Test
    public void movieNameWinsAndIsCapitalized() {
        assertEquals("Боевик", genre("action").name());
        assertEquals("Фэнтези", genre("fantasy").name());
        assertEquals("Мультфильм", genre("animation").name());
    }

    @Test
    public void oneTypeGenresHaveNoIdForTheOtherType() {
        Genre horror = genre("horror");
        assertEquals(Integer.valueOf(27), horror.idFor(MediaType.MOVIE));
        assertNull(horror.idFor(MediaType.TV));
        assertEquals(Set.of(MediaType.MOVIE), horror.mediaTypes());

        Genre kids = genre("kids");
        assertNull(kids.movieGenreId());
        assertEquals(Integer.valueOf(10762), kids.tvGenreId());
        assertEquals("Детский", kids.name());
    }

    @Test
    public void filterSelectsGenresAvailableForTheType() {
        assertTrue(genre("horror").matches(MediaFilter.ALL));
        assertTrue(genre("horror").matches(MediaFilter.MOVIES));
        assertFalse(genre("horror").matches(MediaFilter.SERIES));
        assertFalse(genre("kids").matches(MediaFilter.MOVIES));
        assertTrue(genre("drama").matches(MediaFilter.SERIES));
    }

    @Test
    public void everyCurrentTmdbGenreIsCoveredByTheTable() {
        assertTrue(catalog.stream().noneMatch(it -> it.key().startsWith("movie-") || it.key().startsWith("tv-")));
        Set<MediaKey> covered = new HashSet<>();
        for (Genre genre : catalog) {
            if (genre.movieGenreId() != null) {
                covered.add(new MediaKey(MediaType.MOVIE, genre.movieGenreId()));
            }
            if (genre.tvGenreId() != null) {
                covered.add(new MediaKey(MediaType.TV, genre.tvGenreId()));
            }
        }
        Set<MediaKey> expected = new HashSet<>();
        for (TmdbGenre genre : all()) {
            expected.add(new MediaKey(genre.mediaType(), genre.id()));
        }
        assertEquals(expected, covered);
    }

    @Test
    public void keysAreUniqueAndStable() {
        assertEquals(catalog.size(), catalog.stream().map(Genre::key).distinct().count());
        assertEquals(GenreCatalog.MAPPINGS.size(),
                GenreCatalog.MAPPINGS.stream().map(GenreCatalog.Mapping::key).distinct().count());
        assertEquals("action", catalog.get(0).key());
    }

    @Test
    public void idsMissingFromTmdbAnswerAreDropped() {
        List<Genre> onlyMovies = GenreCatalog.merge(movieGenres);

        assertNull(onlyMovies.stream().filter(it -> it.key().equals("action")).findFirst().orElseThrow().tvGenreId());
        assertTrue(onlyMovies.stream().noneMatch(it -> it.key().equals("kids")));
    }

    @Test
    public void unknownTmdbGenreBecomesASeparateGenre() {
        List<Genre> merged = GenreCatalog.merge(List.of(new TmdbGenre(MediaType.TV, 99_999, "аниме")));

        assertEquals(List.of(new Genre("tv-99999", "Аниме", null, 99_999)), merged);
    }

    @Test
    public void noDataGivesAnEmptyCatalog() {
        assertTrue(GenreCatalog.merge(List.of()).isEmpty());
    }
}

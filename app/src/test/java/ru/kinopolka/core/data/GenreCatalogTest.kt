package ru.kinopolka.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.data.mapper.toTmdbGenres
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.TmdbGenre
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.testing.Fixtures

class GenreCatalogTest {

    private val movieGenres = Fixtures.parse<GenreListResponseDto>(
        "genre_movie_list.json",
    ).toTmdbGenres(MediaType.MOVIE)
    private val tvGenres = Fixtures.parse<GenreListResponseDto>("genre_tv_list.json").toTmdbGenres(MediaType.TV)
    private val catalog = GenreCatalog.merge(movieGenres + tvGenres)

    private fun genre(key: String): Genre = catalog.single { it.key == key }

    @Test
    fun `genres with equal ids are merged`() {
        val comedy = genre("comedy")
        assertEquals(35, comedy.movieGenreId)
        assertEquals(35, comedy.tvGenreId)
        assertEquals("Комедия", comedy.name)
    }

    @Test
    fun `genres with different ids are linked by the table`() {
        assertEquals(28 to 10759, genre("action").let { it.movieGenreId to it.tvGenreId })
        assertEquals(14 to 10765, genre("fantasy").let { it.movieGenreId to it.tvGenreId })
        assertEquals(10752 to 10768, genre("war").let { it.movieGenreId to it.tvGenreId })
    }

    @Test
    fun `movie name wins and is capitalized`() {
        assertEquals("Боевик", genre("action").name)
        assertEquals("Фэнтези", genre("fantasy").name)
        assertEquals("Мультфильм", genre("animation").name)
    }

    @Test
    fun `one-type genres have no id for the other type`() {
        val horror = genre("horror")
        assertEquals(27, horror.idFor(MediaType.MOVIE))
        assertNull(horror.idFor(MediaType.TV))
        assertEquals(setOf(MediaType.MOVIE), horror.mediaTypes)

        val kids = genre("kids")
        assertNull(kids.movieGenreId)
        assertEquals(10762, kids.tvGenreId)
        assertEquals("Детский", kids.name)
    }

    @Test
    fun `filter selects genres available for the type`() {
        assertTrue(genre("horror").matches(MediaFilter.ALL))
        assertTrue(genre("horror").matches(MediaFilter.MOVIES))
        assertFalse(genre("horror").matches(MediaFilter.SERIES))
        assertFalse(genre("kids").matches(MediaFilter.MOVIES))
        assertTrue(genre("drama").matches(MediaFilter.SERIES))
    }

    @Test
    fun `every current TMDB genre is covered by the table`() {
        assertTrue(catalog.none { it.key.startsWith("movie-") || it.key.startsWith("tv-") })
        val covered = catalog.flatMap {
            listOfNotNull(
                it.movieGenreId?.let { id -> MediaType.MOVIE to id },
                it.tvGenreId?.let { id ->
                    MediaType.TV to
                        id
                },
            )
        }.toSet()
        assertEquals((movieGenres + tvGenres).map { it.mediaType to it.id }.toSet(), covered)
    }

    @Test
    fun `keys are unique and stable`() {
        assertEquals(catalog.size, catalog.map { it.key }.toSet().size)
        assertEquals(GenreCatalog.mappings.size, GenreCatalog.mappings.map { it.key }.toSet().size)
        assertEquals("action", catalog.first().key)
    }

    @Test
    fun `ids missing from TMDB answer are dropped`() {
        val onlyMovies = GenreCatalog.merge(movieGenres)

        assertNull(onlyMovies.single { it.key == "action" }.tvGenreId)
        assertTrue(onlyMovies.none { it.key == "kids" })
    }

    @Test
    fun `unknown TMDB genre becomes a separate genre`() {
        val merged = GenreCatalog.merge(listOf(TmdbGenre(MediaType.TV, 99_999, "аниме")))

        assertEquals(listOf(Genre(key = "tv-99999", name = "Аниме", movieGenreId = null, tvGenreId = 99_999)), merged)
    }

    @Test
    fun `no data gives an empty catalog`() {
        assertTrue(GenreCatalog.merge(emptyList()).isEmpty())
    }
}

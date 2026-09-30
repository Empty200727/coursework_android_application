package ru.kinopolka.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.data.GenreCatalog
import ru.kinopolka.core.data.mapper.toTmdbGenres
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.testing.Fixtures

class HomeShelvesTest {

    private val genres = GenreCatalog.merge(
        Fixtures.parse<GenreListResponseDto>("genre_movie_list.json").toTmdbGenres(MediaType.MOVIE) +
            Fixtures.parse<GenreListResponseDto>("genre_tv_list.json").toTmdbGenres(MediaType.TV),
    )

    private fun genreKeys(shelves: List<Shelf>) = shelves.filterIsInstance<Shelf.ByGenre>().map { it.genre.key }

    @Test
    fun `all shows trending, both popular shelves and 8 genres`() {
        val shelves = homeShelves(MediaFilter.ALL, genres)

        assertEquals(
            listOf(Shelf.Trending(MediaFilter.ALL), Shelf.Popular(MediaType.MOVIE), Shelf.Popular(MediaType.TV)),
            shelves.take(3),
        )
        assertEquals(
            listOf("action", "comedy", "drama", "animation", "science_fiction", "crime", "horror", "family"),
            genreKeys(shelves),
        )
        assertTrue(shelves.filterIsInstance<Shelf.ByGenre>().all { it.filter == MediaFilter.ALL })
    }

    @Test
    fun `series skip popular movies and genres without series`() {
        val shelves = homeShelves(MediaFilter.SERIES, genres)

        assertEquals(listOf(Shelf.Trending(MediaFilter.SERIES), Shelf.Popular(MediaType.TV)), shelves.take(2))
        assertTrue("horror" !in genreKeys(shelves))
        assertEquals(8, genreKeys(shelves).size)
    }

    @Test
    fun `movies skip popular series`() {
        val shelves = homeShelves(MediaFilter.MOVIES, genres)

        assertTrue(Shelf.Popular(MediaType.TV) !in shelves)
        assertTrue(Shelf.Popular(MediaType.MOVIE) in shelves)
    }

    @Test
    fun `without genres only collections are shown`() {
        assertEquals(3, homeShelves(MediaFilter.ALL, emptyList()).size)
    }
}

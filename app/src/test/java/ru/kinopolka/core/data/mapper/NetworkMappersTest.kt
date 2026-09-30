package ru.kinopolka.core.data.mapper

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.PagedResponseDto
import ru.kinopolka.core.network.model.TvDetailsDto
import ru.kinopolka.testing.Fixtures

class NetworkMappersTest {

    @Test
    fun `search multi drops people and maps movies and series`() {
        val response: PagedResponseDto<MediaListItemDto> = Fixtures.parse("search_multi.json")

        val media = response.toMediaList()

        assertEquals(
            listOf(MediaKey(MediaType.TV, 1396), MediaKey(MediaType.MOVIE, 559969)),
            media.map { it.key },
        )
        val series = media[0]
        assertEquals("Во все тяжкие", series.title)
        assertEquals("Breaking Bad", series.originalTitle)
        assertEquals(LocalDate.of(2008, 1, 20), series.releaseDate)
        assertEquals(2008, series.releaseYear)
        assertEquals(8.9, series.voteAverage, 0.0)
        assertEquals(15432, series.voteCount)
        assertEquals(listOf(18, 80), series.genreIds)
        assertEquals("/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg", series.posterPath)
    }

    @Test
    fun `empty overview becomes null`() {
        val response: PagedResponseDto<MediaListItemDto> = Fixtures.parse("search_multi.json")

        val movie = response.toMediaList().single { it.mediaType == MediaType.MOVIE }

        assertNull(movie.overview)
        assertEquals(2019, movie.releaseYear)
    }

    @Test
    fun `list without media_type uses the default type`() {
        val movies: PagedResponseDto<MediaListItemDto> = Fixtures.parse("movie_popular.json")
        val series: PagedResponseDto<MediaListItemDto> = Fixtures.parse("tv_popular.json")

        assertEquals(MediaType.MOVIE, movies.toMediaList(MediaType.MOVIE).first().mediaType)
        assertEquals(MediaType.TV, series.toMediaList(MediaType.TV).single().mediaType)
        assertTrue(movies.toMediaList(defaultType = null).isEmpty())
    }

    @Test
    fun `missing title falls back to original title and empty date to null`() {
        val movies: PagedResponseDto<MediaListItemDto> = Fixtures.parse("movie_popular.json")

        val untitled = movies.toMediaList(MediaType.MOVIE).single { it.tmdbId == 1234567 }

        assertEquals("Без даты", untitled.title)
        assertNull(untitled.releaseDate)
        assertNull(untitled.releaseYear)
        assertNull(untitled.posterPath)
        assertEquals(0.0, untitled.voteAverage, 0.0)
    }

    @Test
    fun `items without id or any title are skipped`() {
        assertNull(MediaListItemDto(id = null, mediaType = "movie", title = "Без id").toMediaOrNull())
        assertNull(MediaListItemDto(id = 1, mediaType = "movie", title = " ").toMediaOrNull())
        assertNull(MediaListItemDto(id = 1, mediaType = "collection", title = "Коллекция").toMediaOrNull())
    }

    @Test
    fun `year is parsed from the date`() {
        assertEquals(LocalDate.of(1999, 10, 15), "1999-10-15".toLocalDateOrNull())
        assertNull("".toLocalDateOrNull())
        assertNull("1999".toLocalDateOrNull())
        assertNull(null.toLocalDateOrNull())
    }

    @Test
    fun `genre list maps names and skips broken entries`() {
        val response: GenreListResponseDto = Fixtures.parse("genre_movie_list.json")

        val genres = response.toTmdbGenres(MediaType.MOVIE)

        assertEquals(19, genres.size)
        assertTrue(genres.all { it.mediaType == MediaType.MOVIE })
        assertEquals(
            emptyList<Any>(),
            GenreListResponseDto(genres = listOf(ru.kinopolka.core.network.model.GenreDto(id = 1, name = "")))
                .toTmdbGenres(MediaType.TV),
        )
    }

    @Test
    fun `movie details map runtime, genres, top 10 cast and related titles`() {
        val dto: MovieDetailsDto = Fixtures.parse("movie_details_550.json")

        val details = requireNotNull(dto.toMediaDetails())

        assertEquals(MediaKey(MediaType.MOVIE, 550), details.media.key)
        assertEquals("Бойцовский клуб", details.media.title)
        assertEquals("Fight Club", details.media.originalTitle)
        assertEquals(139, details.runtimeMinutes)
        assertNull(details.numberOfSeasons)
        assertEquals(listOf("драма", "триллер"), details.genres.map { it.name })
        assertEquals(listOf(18, 53), details.media.genreIds)
        assertFalse(details.isOverviewFallback)

        assertEquals(MAX_CAST_MEMBERS, details.cast.size)
        assertEquals("Эдвард Нортон", details.cast.first().name)
        assertEquals("Рассказчик", details.cast.first().character)
        assertEquals("Тайлер Дёрден", details.cast.single { it.personId == 287 }.character)
        assertEquals((0..9).toList(), details.cast.map { it.order })

        assertEquals(listOf(807, 680), details.recommendations.map { it.tmdbId })
        assertEquals(listOf(MediaKey(MediaType.MOVIE, 1954)), details.similar.map { it.key })
    }

    @Test
    fun `tv details map seasons, air dates and the main role`() {
        val dto: TvDetailsDto = Fixtures.parse("tv_details_1396.json")

        val details = requireNotNull(dto.toMediaDetails())

        assertEquals(MediaKey(MediaType.TV, 1396), details.media.key)
        assertEquals(2008, details.media.releaseYear)
        assertEquals(LocalDate.of(2013, 9, 29), details.lastAirDate)
        assertEquals(5, details.numberOfSeasons)
        assertEquals(false, details.inProduction)
        assertNull(details.runtimeMinutes)
        assertNull(details.media.overview)
        assertEquals("Джесси Пинкман", details.cast.single { it.personId == 84497 }.character)
        assertEquals(
            listOf(MediaKey(MediaType.TV, 60059), MediaKey(MediaType.MOVIE, 559969)),
            details.recommendations.map { it.key },
        )
        assertTrue(details.similar.isEmpty())
    }

    @Test
    fun `details without id are rejected`() {
        assertNull(MovieDetailsDto(title = "Без id").toMediaDetails())
        assertNull(TvDetailsDto(name = "Без id").toMediaDetails())
    }
}

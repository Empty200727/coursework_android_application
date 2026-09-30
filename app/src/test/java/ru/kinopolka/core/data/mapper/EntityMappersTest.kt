package ru.kinopolka.core.data.mapper

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind
import ru.kinopolka.core.model.TmdbGenre
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.TvDetailsDto
import ru.kinopolka.testing.Fixtures

class EntityMappersTest {

    private val media = Media(
        key = MediaKey(MediaType.TV, 1396),
        title = "Во все тяжкие",
        originalTitle = "Breaking Bad",
        overview = "Учитель химии",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = LocalDate.of(2008, 1, 20),
        voteAverage = 8.9,
        voteCount = 15432,
        popularity = 312.5,
        genreIds = listOf(18, 80, 18),
    )

    @Test
    fun `media survives the round trip through the entity`() {
        val entity = media.toEntity(cachedAt = 42L)

        assertEquals(MediaType.TV, entity.mediaType)
        assertEquals("2008-01-20", entity.releaseDate)
        assertEquals(42L, entity.cachedAt)
        assertNull(entity.detailsCachedAt)
        assertEquals(media.copy(genreIds = listOf(18, 80)), entity.toMedia(genreIds = listOf(18, 80)))
    }

    @Test
    fun `genre links are unique`() {
        assertEquals(listOf(18, 80), media.toGenreEntities().map { it.genreId })
        assertTrue(media.toGenreEntities().all { it.mediaType == MediaType.TV && it.tmdbId == 1396 })
    }

    @Test
    fun `movie details fill the detail columns`() {
        val details = requireNotNull(Fixtures.parse<MovieDetailsDto>("movie_details_550.json").toMediaDetails())

        val entity = details.toEntity(cachedAt = 100L)

        assertEquals(139, entity.runtime)
        assertNull(entity.numberOfSeasons)
        assertEquals(100L, entity.detailsCachedAt)
        assertFalse(entity.isOverviewFallback)
    }

    @Test
    fun `tv details fill seasons and last air date`() {
        val details = requireNotNull(Fixtures.parse<TvDetailsDto>("tv_details_1396.json").toMediaDetails())

        val entity = details.toEntity(cachedAt = 100L)

        assertEquals(5, entity.numberOfSeasons)
        assertEquals("2013-09-29", entity.lastAirDate)
        assertEquals(false, entity.inProduction)
        assertNull(entity.runtime)
    }

    @Test
    fun `cast, genres and library entries survive the round trip`() {
        val cast =
            CastMember(
                personId = 17419,
                name = "Брайан Крэнстон",
                character = "Уолтер Уайт",
                profilePath = null,
                order = 0,
            )
        assertEquals(cast, cast.toEntity(media.key).toCastMember())

        val genre = TmdbGenre(MediaType.TV, 10765, "НФ и Фэнтези")
        assertEquals(genre, genre.toEntity(cachedAt = 1L).toTmdbGenre())

        val entry = LibraryEntry(
            key = media.key,
            watchStatus = WatchStatus.WATCHED,
            isFavorite = true,
            addedAt = Instant.ofEpochMilli(1_000),
            watchedAt = Instant.ofEpochMilli(2_000),
            userRating = 9,
            localPosterPath = "/data/posters/tv_1396.jpg",
        )
        assertEquals(entry, entry.toEntity().toLibraryEntry())
    }

    @Test
    fun `related and feed items keep order and drop duplicates`() {
        val other = media.copy(key = MediaKey(MediaType.MOVIE, 1396))

        val related = relatedEntities(MediaKey(MediaType.MOVIE, 550), RelatedKind.SIMILAR, listOf(media, other, media))
        assertEquals(listOf(0, 1), related.map { it.position })
        assertEquals(listOf(MediaType.TV, MediaType.MOVIE), related.map { it.targetMediaType })

        val feed = feedItemEntities("trending", listOf(other, media, other), fetchedAt = 5L)
        assertEquals(listOf(MediaType.MOVIE, MediaType.TV), feed.map { it.mediaType })
        assertTrue(feed.all { it.feedKey == "trending" && it.fetchedAt == 5L })
    }

    @Test
    fun `library entry without status and favorite is empty`() {
        val entry = LibraryEntry(media.key, WatchStatus.NONE, false, Instant.EPOCH, null, null, null)
        assertTrue(entry.isEmpty)
        assertFalse(entry.copy(isFavorite = true).isEmpty)
        assertFalse(entry.copy(watchStatus = WatchStatus.WANT).isEmpty)
    }
}

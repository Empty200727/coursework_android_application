package ru.kinopolka.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.mapper.toEntity
import ru.kinopolka.core.data.mapper.toTmdbGenres
import ru.kinopolka.core.database.DatabaseTest
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.PagedResponseDto
import ru.kinopolka.core.network.model.TvDetailsDto
import ru.kinopolka.testing.FakeTmdbApi
import ru.kinopolka.testing.Fixtures
import ru.kinopolka.testing.TestTimeProvider

/** Title card: one request, cache of 24 hours (N-03), similar as a fallback (F-10), English overview (N-07). */
@RunWith(AndroidJUnit4::class)
class OfflineFirstDetailsRepositoryTest : DatabaseTest() {

    private val api = FakeTmdbApi()
    private val clock = TestTimeProvider()
    private val fightClub = MediaKey(MediaType.MOVIE, 550)
    private val breakingBad = MediaKey(MediaType.TV, 1396)
    private val repository by lazy {
        OfflineFirstDetailsRepository(
            api,
            database,
            database.mediaDao(),
            database.castDao(),
            database.relatedMediaDao(),
            database.genreDao(),
            clock,
        )
    }

    @Before
    fun cacheGenres() = runTest {
        val genres = Fixtures.parse<GenreListResponseDto>("genre_movie_list.json").toTmdbGenres(MediaType.MOVIE) +
            Fixtures.parse<GenreListResponseDto>("genre_tv_list.json").toTmdbGenres(MediaType.TV)
        database.genreDao().upsert(genres.map { it.toEntity(cachedAt = 0L) })
    }

    @Test
    fun `movie card is loaded in one request and read from the database`() = runTest {
        assertNull(repository.observeDetails(fightClub).first())

        assertEquals(RefreshResult.Updated, repository.refreshDetails(fightClub))

        assertEquals(listOf("movie/550:credits,recommendations,similar:"), api.requests)
        val details = requireNotNull(repository.observeDetails(fightClub).first())
        assertEquals("Бойцовский клуб", details.media.title)
        assertEquals(139, details.runtimeMinutes)
        assertEquals(listOf("Драма", "Триллер"), details.genres.map { it.name })
        assertEquals(10, details.cast.size)
        assertEquals("Эдвард Нортон", details.cast.first().name)
        assertEquals("recommendations come first", listOf(807, 680), details.related.map { it.tmdbId })
        assertFalse(details.isOverviewFallback)
        assertTrue(details.isComplete)
    }

    @Test
    fun `similar titles are shown when there are no recommendations`() = runTest {
        api.movieDetails = { _, _ ->
            Fixtures.parse<MovieDetailsDto>(
                "movie_details_550.json",
            ).copy(recommendations = PagedResponseDto(results = emptyList()))
        }

        repository.refreshDetails(fightClub)

        val details = requireNotNull(repository.observeDetails(fightClub).first())
        assertTrue(details.recommendations.isEmpty())
        assertEquals(listOf(MediaKey(MediaType.MOVIE, 1954)), details.related.map { it.key })
    }

    @Test
    fun `missing russian overview is replaced by the english one`() = runTest {
        api.tvDetails = { _, language ->
            val dto = Fixtures.parse<TvDetailsDto>("tv_details_1396.json")
            if (language == "en-US") dto.copy(overview = "A chemistry teacher turns to crime.") else dto
        }

        repository.refreshDetails(breakingBad)

        assertEquals(
            listOf("tv/1396:aggregate_credits,recommendations,similar:", "tv/1396::en-US"),
            api.requests,
        )
        val details = requireNotNull(repository.observeDetails(breakingBad).first())
        assertEquals("A chemistry teacher turns to crime.", details.media.overview)
        assertTrue(details.isOverviewFallback)
        assertEquals(5, details.numberOfSeasons)
        assertEquals("Джесси Пинкман", details.cast.single { it.personId == 84497 }.character)
    }

    @Test
    fun `card is cached for 24 hours`() = runTest {
        repository.refreshDetails(fightClub)

        clock.advanceBy(23.hours)
        assertEquals(RefreshResult.Skipped, repository.refreshDetails(fightClub))

        clock.advanceBy(1.hours)
        assertEquals(RefreshResult.Updated, repository.refreshDetails(fightClub))
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `offline the cached card stays available`() = runTest {
        repository.refreshDetails(fightClub)
        clock.advanceBy(25.hours)
        api.failure = IOException("offline")

        assertEquals(RefreshResult.Failed(DataError.NO_CONNECTION), repository.refreshDetails(fightClub))
        assertEquals(10, repository.observeDetails(fightClub).first()?.cast?.size)
    }

    @Test
    fun `title known from a list is shown before its card is loaded`() = runTest {
        database.mediaDao().upsertSummaries(listOf(media(550)))

        val details = requireNotNull(repository.observeDetails(fightClub).first())

        assertFalse(details.isComplete)
        assertTrue(details.cast.isEmpty())
    }

    @Test
    fun `unknown title is reported`() = runTest {
        api.failure = FakeTmdbApi.httpError(404)

        assertEquals(RefreshResult.Failed(DataError.NOT_FOUND), repository.refreshDetails(fightClub))
    }
}

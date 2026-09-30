package ru.kinopolka.core.data.repository

import app.cash.turbine.test
import java.io.IOException
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.GenreCatalog
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.testing.FakeGenreDao
import ru.kinopolka.testing.FakeTmdbApi
import ru.kinopolka.testing.TestTimeProvider

class OfflineFirstGenreRepositoryTest {

    private val api = FakeTmdbApi()
    private val dao = FakeGenreDao()
    private val clock = TestTimeProvider()
    private val repository = OfflineFirstGenreRepository(api, dao, clock)

    @Test
    fun `empty cache is loaded from both genre endpoints`() = runTest {
        assertTrue(repository.observeGenres().first().isEmpty())

        val result = repository.refresh()

        assertEquals(RefreshResult.Updated, result)
        assertEquals(2, api.genreRequests)
        val genres = repository.observeGenres().first()
        assertEquals("Боевик", genres.single { it.key == "action" }.name)
        assertEquals(clock.now, dao.oldestCachedAt(MediaType.MOVIE))
        assertEquals(clock.now, dao.oldestCachedAt(MediaType.TV))
    }

    @Test
    fun `observers receive the catalog once it is loaded`() = runTest {
        repository.observeGenres().test {
            assertEquals(emptyList<Any>(), awaitItem())
            repository.refresh()
            // Movie and series genres are written separately: the last value is the full catalog.
            assertEquals(GenreCatalog.mappings.map { it.key }, expectMostRecentItem().map { it.key })
        }
    }

    @Test
    fun `fresh cache is not reloaded`() = runTest {
        repository.refresh()
        clock.advanceBy(6.days)

        assertEquals(RefreshResult.Skipped, repository.refresh())
        assertEquals(2, api.genreRequests)
    }

    @Test
    fun `cache older than 7 days is reloaded`() = runTest {
        repository.refresh()
        clock.advanceBy(7.days)

        assertEquals(RefreshResult.Updated, repository.refresh())
        assertEquals(4, api.genreRequests)
        assertEquals(clock.now, dao.oldestCachedAt(MediaType.MOVIE))
    }

    @Test
    fun `forced refresh ignores the cache age`() = runTest {
        repository.refresh()

        assertEquals(RefreshResult.Updated, repository.refresh(force = true))
        assertEquals(4, api.genreRequests)
    }

    @Test
    fun `missing genres of one type make the cache stale`() = runTest {
        api.tvGenres = GenreListResponseDto(genres = emptyList())
        repository.refresh()
        api.tvGenres = FakeTmdbApi().tvGenres

        assertEquals(RefreshResult.Updated, repository.refresh())
        assertEquals(10762, repository.getGenre("kids")?.tvGenreId)
    }

    @Test
    fun `failed refresh keeps cached genres`() = runTest {
        repository.refresh()
        clock.advanceBy(8.days)
        api.failure = IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.NO_CONNECTION), result)
        assertEquals(GenreCatalog.mappings.size, repository.observeGenres().first().size)
    }

    @Test
    fun `server errors are classified`() = runTest {
        api.failure = FakeTmdbApi.httpError(401)
        assertEquals(RefreshResult.Failed(DataError.UNAUTHORIZED), repository.refresh())

        api.failure = FakeTmdbApi.httpError(503)
        assertEquals(RefreshResult.Failed(DataError.SERVER), repository.refresh())

        api.failure = SerializationException("broken json")
        assertEquals(RefreshResult.Failed(DataError.BAD_RESPONSE), repository.refresh())
    }

    @Test
    fun `empty answer does not wipe the catalog`() = runTest {
        repository.refresh()
        clock.advanceBy(8.days)
        api.movieGenres = GenreListResponseDto(genres = null)
        api.tvGenres = GenreListResponseDto(genres = emptyList())

        repository.refresh()

        assertEquals(GenreCatalog.mappings.size, repository.observeGenres().first().size)
    }

    @Test
    fun `genres are filtered by media type`() = runTest {
        repository.refresh()

        val series = repository.observeGenres(MediaFilter.SERIES).first()
        val movies = repository.observeGenres(MediaFilter.MOVIES).first()

        assertTrue(series.none { it.key == "horror" })
        assertTrue(series.any { it.key == "kids" })
        assertTrue(movies.none { it.key == "kids" })
        assertTrue(movies.any { it.key == "horror" })
    }

    @Test
    fun `genre is found by key`() = runTest {
        repository.refresh()

        assertEquals(27, repository.getGenre("horror")?.movieGenreId)
        assertNull(repository.getGenre("unknown"))
    }
}

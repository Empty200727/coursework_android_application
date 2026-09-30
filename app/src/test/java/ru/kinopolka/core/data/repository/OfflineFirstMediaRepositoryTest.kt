package ru.kinopolka.core.data.repository

import androidx.paging.testing.asSnapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.database.DatabaseTest
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.testing.FakeTmdbApi
import ru.kinopolka.testing.TestTimeProvider

/** Shelves are cached in `feed_item` for 6 hours (N-03), checked with a controllable clock. */
@RunWith(AndroidJUnit4::class)
class OfflineFirstMediaRepositoryTest : DatabaseTest() {

    private val api = FakeTmdbApi()
    private val clock = TestTimeProvider()
    private val action = Genre(key = "action", name = "Боевик", movieGenreId = 28, tvGenreId = 10759)
    private val horror = Genre(key = "horror", name = "Ужасы", movieGenreId = 27, tvGenreId = null)

    private val repository by lazy {
        OfflineFirstMediaRepository(api, database, database.mediaDao(), database.feedDao(), clock)
    }

    @Test
    fun `shelf is empty until loaded and then read from the database`() = runTest {
        val shelf = Shelf.Trending(MediaFilter.ALL)
        assertTrue(repository.observeShelf(shelf).first().isEmpty())

        assertEquals(RefreshResult.Updated, repository.refreshShelf(shelf))

        assertEquals(
            listOf(MediaKey(MediaType.MOVIE, 693134), MediaKey(MediaType.TV, 94997)),
            repository.observeShelf(shelf).first().map { it.key },
        )
        assertEquals(listOf("trending/all/week"), api.requests)
    }

    @Test
    fun `fresh shelf is not reloaded within 6 hours`() = runTest {
        val shelf = Shelf.Popular(MediaType.MOVIE)
        repository.refreshShelf(shelf)

        clock.advanceBy(6.hours - 1.minutes)
        assertEquals(RefreshResult.Skipped, repository.refreshShelf(shelf))
        assertEquals(1, api.requests.size)

        clock.advanceBy(1.minutes)
        assertEquals(RefreshResult.Updated, repository.refreshShelf(shelf))
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `forced refresh ignores the cache age`() = runTest {
        val shelf = Shelf.Popular(MediaType.TV)
        repository.refreshShelf(shelf)

        assertEquals(RefreshResult.Updated, repository.refreshShelf(shelf, force = true))
        assertEquals(listOf("tv/popular", "tv/popular"), api.requests)
    }

    @Test
    fun `failed refresh keeps the cached shelf`() = runTest {
        val shelf = Shelf.Trending(MediaFilter.ALL)
        repository.refreshShelf(shelf)
        clock.advanceBy(7.hours)
        api.failure = IOException("offline")

        assertEquals(RefreshResult.Failed(DataError.NO_CONNECTION), repository.refreshShelf(shelf))
        assertEquals(2, repository.observeShelf(shelf).first().size)
    }

    @Test
    fun `genre shelf for all merges movies and series by popularity`() = runTest {
        val shelf = Shelf.ByGenre(action, MediaFilter.ALL)

        repository.refreshShelf(shelf)

        // discover_movie: «Матрица» 85.3; discover_tv: «Дом Дракона» 980.2, «Пацаны» 40.5.
        assertEquals(
            listOf(MediaKey(MediaType.TV, 94997), MediaKey(MediaType.MOVIE, 603), MediaKey(MediaType.TV, 76479)),
            repository.observeShelf(shelf).first().map { it.key },
        )
        assertEquals(
            setOf("discover/movie:28:popularity.desc:1", "discover/tv:10759:popularity.desc:1"),
            api.requests.toSet(),
        )
    }

    @Test
    fun `filtered genre shelf uses one type and shares the cache with all`() = runTest {
        repository.refreshShelf(Shelf.ByGenre(action, MediaFilter.ALL))

        assertEquals(RefreshResult.Skipped, repository.refreshShelf(Shelf.ByGenre(action, MediaFilter.SERIES)))
        assertEquals(
            listOf(94997, 76479),
            repository.observeShelf(Shelf.ByGenre(action, MediaFilter.SERIES)).first().map { it.tmdbId },
        )
    }

    @Test
    fun `genre without series shows only movies`() = runTest {
        val shelf = Shelf.ByGenre(horror, MediaFilter.ALL)

        repository.refreshShelf(shelf)

        assertEquals(listOf("discover/movie:27:popularity.desc:1"), api.requests)
        assertEquals(listOf(603), repository.observeShelf(shelf).first().map { it.tmdbId })
    }

    @Test
    fun `search results come from the network without people`() = runTest {
        val results = repository.search("тяжкие", MediaFilter.ALL).asSnapshot()

        assertEquals(listOf(MediaKey(MediaType.TV, 1396), MediaKey(MediaType.MOVIE, 559969)), results.map { it.key })
    }
}

package ru.kinopolka.core.data.cleanup

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.database.DatabaseTest
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.FakePosterStorage
import ru.kinopolka.testing.TestTimeProvider

/** The daily cleanup removes old cache but never the library (docs/PLAN.md, section 6). */
@RunWith(AndroidJUnit4::class)
class CacheCleanerTest : DatabaseTest() {

    private val clock = TestTimeProvider()
    private val posters = FakePosterStorage()
    private val cleaner by lazy { CacheCleaner(database.mediaDao(), database.libraryDao(), posters, clock) }

    @Test
    fun `old cache is removed, library and fresh cache stay`() = runTest {
        val old = clock.now
        database.mediaDao().upsert(listOf(media(1, cachedAt = old), media(2, cachedAt = old)))
        database.libraryDao().upsert(
            LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WANT, false, old, null, null, "posters/movie_2.jpg"),
        )
        clock.advanceBy(31.days)
        database.mediaDao().upsert(listOf(media(3, cachedAt = clock.now)))
        posters.files += setOf("posters/movie_2.jpg", "posters/movie_9.jpg")

        val result = cleaner.clean()

        assertEquals(CacheCleaner.Result(removedTitles = 1, removedPosters = 1), result)
        assertNull(database.mediaDao().get(MediaType.MOVIE, 1))
        assertNotNull("library title", database.mediaDao().get(MediaType.MOVIE, 2))
        assertNotNull("fresh title", database.mediaDao().get(MediaType.MOVIE, 3))
        assertEquals(setOf("posters/movie_2.jpg"), posters.files)
    }

    @Test
    fun `cache younger than 30 days is kept`() = runTest {
        database.mediaDao().upsert(listOf(media(1, cachedAt = clock.now)))
        clock.advanceBy(29.days)

        assertEquals(0, cleaner.clean().removedTitles)
    }
}

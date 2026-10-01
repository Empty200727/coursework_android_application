package ru.kinopolka.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.database.DatabaseTest
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.FakePosterStorage
import ru.kinopolka.testing.TestTimeProvider

@RunWith(AndroidJUnit4::class)
class OfflineFirstLibraryRepositoryTest : DatabaseTest() {

    private val posters = FakePosterStorage()
    private val clock = TestTimeProvider()
    private val key = MediaKey(MediaType.MOVIE, 550)
    private val repository by lazy {
        OfflineFirstLibraryRepository(database, database.libraryDao(), database.mediaDao(), posters, clock)
    }

    private suspend fun cacheTitle(id: Int = 550, type: MediaType = MediaType.MOVIE) {
        database.mediaDao().upsert(listOf(media(id, type)))
    }

    @Test
    fun `adding a title saves its poster`() = runTest {
        cacheTitle()

        repository.setWatchStatus(key, WatchStatus.WANT)

        val entry = requireNotNull(repository.observeEntry(key).first())
        assertEquals(WatchStatus.WANT, entry.watchStatus)
        assertEquals("posters/movie_550.jpg", entry.localPosterPath)
        assertEquals(listOf(key), repository.observeItems().first().map { it.media.key })
    }

    @Test
    fun `want and watched exclude each other, favorite is independent`() = runTest {
        cacheTitle()

        repository.setWatchStatus(key, WatchStatus.WANT)
        repository.setFavorite(key, true)
        repository.setWatchStatus(key, WatchStatus.WATCHED)

        val entry = requireNotNull(repository.observeEntry(key).first())
        assertEquals(WatchStatus.WATCHED, entry.watchStatus)
        assertTrue(entry.isFavorite)
        assertEquals(1, repository.observeItems().first().size)
    }

    @Test
    fun `entry without status and favorite is deleted with its poster`() = runTest {
        cacheTitle()
        repository.setWatchStatus(key, WatchStatus.WANT)
        repository.setFavorite(key, true)

        repository.setFavorite(key, false)
        assertFalse(repository.observeEntry(key).first() == null)

        repository.setWatchStatus(key, WatchStatus.NONE)
        assertNull(repository.observeEntry(key).first())
        assertTrue("poster file removed", posters.files.isEmpty())
    }

    @Test
    fun `removal can be undone and keeps the poster until released`() = runTest {
        cacheTitle()
        repository.setWatchStatus(key, WatchStatus.WANT)

        val previous = requireNotNull(repository.removeFromTab(key, LibraryTab.WANT))
        assertNull(repository.observeEntry(key).first())
        assertEquals(setOf("posters/movie_550.jpg"), posters.files)

        repository.restore(previous)
        assertEquals(previous, repository.observeEntry(key).first())

        repository.removeFromTab(key, LibraryTab.WANT)
        repository.releasePoster(previous)
        assertTrue(posters.files.isEmpty())
    }

    @Test
    fun `releasing does not delete the poster of a title still in the library`() = runTest {
        cacheTitle()
        repository.setWatchStatus(key, WatchStatus.WATCHED)
        repository.setFavorite(key, true)

        val previous = requireNotNull(repository.removeFromTab(key, LibraryTab.FAVORITES))
        repository.releasePoster(previous)

        assertEquals(setOf("posters/movie_550.jpg"), posters.files)
        assertEquals(WatchStatus.WATCHED, repository.observeEntry(key).first()?.watchStatus)
    }

    @Test
    fun `poster is retried later when saving failed`() = runTest {
        cacheTitle()
        posters.failSaving = true
        repository.setWatchStatus(key, WatchStatus.WANT)
        assertNull(repository.observeEntry(key).first()?.localPosterPath)

        posters.failSaving = false
        repository.setFavorite(key, true)

        assertEquals("posters/movie_550.jpg", repository.observeEntry(key).first()?.localPosterPath)
    }

    @Test
    fun `library titles are kept by the cache cleanup`() = runTest {
        cacheTitle()
        repository.setWatchStatus(key, WatchStatus.WANT)

        database.mediaDao().deleteUnusedCachedBefore(Long.MAX_VALUE)

        assertEquals(1, repository.observeItems().first().size)
    }
}

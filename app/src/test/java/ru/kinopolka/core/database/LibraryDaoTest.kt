package ru.kinopolka.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.database.entity.SearchHistoryEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus

/** Library and search history tables. */
@RunWith(AndroidJUnit4::class)
class LibraryDaoTest : DatabaseTest() {

    private fun entry(id: Int, status: WatchStatus, favorite: Boolean = false, addedAt: Long = id.toLong()) =
        LibraryEntryEntity(MediaType.MOVIE, id, status, favorite, addedAt, null, null, null)

    @Test
    fun `entries are selected by status and favorite flag`() = runTest {
        database.mediaDao().upsert(listOf(media(1), media(2), media(3)))
        val dao = database.libraryDao()
        dao.upsert(entry(1, WatchStatus.WANT))
        dao.upsert(entry(2, WatchStatus.WANT, favorite = true))
        dao.upsert(entry(3, WatchStatus.WATCHED, favorite = true))

        assertEquals(listOf(2, 1), dao.observeByStatus(WatchStatus.WANT).first().map { it.tmdbId })
        assertEquals(listOf(3), dao.observeByStatus(WatchStatus.WATCHED).first().map { it.tmdbId })
        assertEquals(listOf(3, 2), dao.observeFavorites().first().map { it.tmdbId })
    }

    @Test
    fun `status change replaces the entry and delete removes it`() = runTest {
        database.mediaDao().upsert(listOf(media(1)))
        val dao = database.libraryDao()
        dao.upsert(entry(1, WatchStatus.WANT))

        dao.upsert(entry(1, WatchStatus.WATCHED).copy(watchedAt = 50L))
        assertEquals(WatchStatus.WATCHED, dao.observeEntry(MediaType.MOVIE, 1).first()?.watchStatus)
        assertEquals(emptyList<Any>(), dao.observeByStatus(WatchStatus.WANT).first())

        dao.delete(MediaType.MOVIE, 1)
        assertNull(dao.get(MediaType.MOVIE, 1))
    }

    @Test
    fun `search history keeps the most recent queries`() = runTest {
        val dao = database.searchHistoryDao()
        (1..12).forEach { dao.upsert(SearchHistoryEntity("запрос $it", searchedAt = it.toLong())) }

        dao.trimTo(keep = 10)

        val recent = dao.observeRecent(limit = 20).first()
        assertEquals(10, recent.size)
        assertEquals("запрос 12", recent.first().query)
        assertEquals("запрос 3", recent.last().query)

        dao.clear()
        assertEquals(emptyList<Any>(), dao.observeRecent(limit = 10).first())
    }
}

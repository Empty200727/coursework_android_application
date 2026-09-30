package ru.kinopolka.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.database.entity.CastMemberEntity
import ru.kinopolka.core.database.entity.FeedItemEntity
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.database.entity.RelatedMediaEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind
import ru.kinopolka.core.model.WatchStatus

@RunWith(AndroidJUnit4::class)
class MediaDaoTest : DatabaseTest() {

    private val mediaDao get() = database.mediaDao()

    @Test
    fun `movie and series with the same id are different rows`() = runTest {
        mediaDao.upsert(listOf(media(1396, MediaType.MOVIE, "Фильм"), media(1396, MediaType.TV, "Сериал")))

        assertEquals("Фильм", mediaDao.get(MediaType.MOVIE, 1396)?.title)
        assertEquals("Сериал", mediaDao.get(MediaType.TV, 1396)?.title)
        assertEquals(2, mediaDao.count())
    }

    @Test
    fun `list data does not erase loaded details`() = runTest {
        mediaDao.upsert(listOf(media(550, runtime = 139, detailsCachedAt = 1_000L)))

        mediaDao.upsertSummaries(listOf(media(550, title = "Бойцовский клуб", cachedAt = 2_000L), media(551)))

        val updated = requireNotNull(mediaDao.get(MediaType.MOVIE, 550))
        assertEquals("Бойцовский клуб", updated.title)
        assertEquals(2_000L, updated.cachedAt)
        assertEquals(139, updated.runtime)
        assertEquals(1_000L, updated.detailsCachedAt)
        assertNotNull(mediaDao.get(MediaType.MOVIE, 551))
    }

    @Test
    fun `observe emits the current row`() = runTest {
        assertNull(mediaDao.observe(MediaType.MOVIE, 550).first())

        mediaDao.upsert(listOf(media(550)))

        assertEquals("Фильм 550", mediaDao.observe(MediaType.MOVIE, 550).first()?.title)
    }

    @Test
    fun `genres of a title are replaced`() = runTest {
        mediaDao.upsert(listOf(media(550)))
        mediaDao.replaceGenres(MediaType.MOVIE, 550, listOf(18, 53, 18))
        assertEquals(setOf(18, 53), mediaDao.observeGenreIds(MediaType.MOVIE, 550).first().toSet())

        mediaDao.replaceGenres(MediaType.MOVIE, 550, listOf(35))
        assertEquals(listOf(35), mediaDao.observeGenreIds(MediaType.MOVIE, 550).first())
    }

    @Test
    fun `cleanup removes old titles with their cache but keeps the library`() = runTest {
        mediaDao.upsert(
            listOf(
                media(1, cachedAt = 100L),
                media(2, cachedAt = 100L),
                media(3, cachedAt = 5_000L),
            ),
        )
        mediaDao.replaceGenres(MediaType.MOVIE, 1, listOf(18))
        database.castDao().insertCast(listOf(CastMemberEntity(MediaType.MOVIE, 1, 10, "Актёр", null, null, 0)))
        database.feedDao().insertItems(listOf(FeedItemEntity("trending", 0, MediaType.MOVIE, 1, 100L)))
        database.relatedMediaDao().insertRelated(
            listOf(RelatedMediaEntity(MediaType.MOVIE, 3, RelatedKind.SIMILAR, MediaType.MOVIE, 1, 0)),
        )
        database.libraryDao().upsert(
            LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WANT, false, 100L, null, null, null),
        )

        val deleted = mediaDao.deleteUnusedCachedBefore(threshold = 1_000L)

        assertEquals(1, deleted)
        assertNull(mediaDao.get(MediaType.MOVIE, 1))
        assertNotNull("library title is kept", mediaDao.get(MediaType.MOVIE, 2))
        assertNotNull("fresh title is kept", mediaDao.get(MediaType.MOVIE, 3))
        assertTrue(mediaDao.observeGenreIds(MediaType.MOVIE, 1).first().isEmpty())
        assertTrue(database.castDao().observeCast(MediaType.MOVIE, 1).first().isEmpty())
        assertTrue(database.feedDao().observeFeed("trending").first().isEmpty())
        assertTrue(database.relatedMediaDao().observeRelated(MediaType.MOVIE, 3, RelatedKind.SIMILAR).first().isEmpty())
        assertNotNull(database.libraryDao().get(MediaType.MOVIE, 2))
    }

    @Test
    fun `a title in the library cannot be deleted`() = runTest {
        mediaDao.upsert(listOf(media(2)))
        database.libraryDao().upsert(
            LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WATCHED, true, 100L, 200L, 9, null),
        )

        val result = runCatching {
            database.openHelper.writableDatabase.execSQL("DELETE FROM media WHERE tmdb_id = 2")
        }

        assertTrue(result.isFailure)
        assertNotNull(mediaDao.get(MediaType.MOVIE, 2))
    }
}

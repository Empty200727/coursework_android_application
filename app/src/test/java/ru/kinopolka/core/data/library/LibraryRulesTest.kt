package ru.kinopolka.core.data.library

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibrarySort
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.testing.testMedia

/** Status rule (docs/PLAN.md, section 2) and list building of «Моя полка» (F-11, F-12). */
class LibraryRulesTest {

    private val key = MediaKey(MediaType.MOVIE, 550)
    private val t1 = Instant.ofEpochSecond(1_000)
    private val t2 = Instant.ofEpochSecond(2_000)

    @Test
    fun `watched replaces want`() {
        val want = null.withWatchStatus(key, WatchStatus.WANT, t1)
        val watched = want.withWatchStatus(key, WatchStatus.WATCHED, t2)

        assertEquals(WatchStatus.WANT, want.watchStatus)
        assertNull(want.watchedAt)
        assertEquals(WatchStatus.WATCHED, watched.watchStatus)
        assertEquals(t2, watched.watchedAt)
        assertEquals(t2, watched.addedAt)
        assertFalse(LibraryTab.WANT.contains(watched))
        assertTrue(LibraryTab.WATCHED.contains(watched))
    }

    @Test
    fun `favorite is independent of the status`() {
        val entry = null.withFavorite(key, true, t1).withWatchStatus(key, WatchStatus.WATCHED, t2)

        assertTrue(entry.isFavorite)
        assertTrue(LibraryTab.FAVORITES.contains(entry))
        assertTrue(LibraryTab.WATCHED.contains(entry))

        val cleared = entry.withWatchStatus(key, WatchStatus.NONE, t2)
        assertTrue("favorite stays", cleared.isFavorite)
        assertFalse(cleared.isEmpty)
    }

    @Test
    fun `entry without status and favorite is empty`() {
        val entry = null.withWatchStatus(key, WatchStatus.WANT, t1).withWatchStatus(key, WatchStatus.NONE, t2)

        assertTrue(entry.isEmpty)
        assertTrue(null.withFavorite(key, true, t1).withFavorite(key, false, t2).isEmpty)
    }

    @Test
    fun `same status again keeps the entry unchanged`() {
        val want = null.withWatchStatus(key, WatchStatus.WANT, t1)

        assertEquals(want, want.withWatchStatus(key, WatchStatus.WANT, t2))
    }

    @Test
    fun `removal from a tab clears only that tab`() {
        val entry = LibraryEntry(key, WatchStatus.WATCHED, true, t1, t1, 8, "/p.jpg")

        val fromWatched = entry.removedFrom(LibraryTab.WATCHED)
        assertEquals(WatchStatus.NONE, fromWatched.watchStatus)
        assertTrue(fromWatched.isFavorite)

        val fromFavorites = entry.removedFrom(LibraryTab.FAVORITES)
        assertEquals(WatchStatus.WATCHED, fromFavorites.watchStatus)
        assertFalse(fromFavorites.isFavorite)
    }

    private fun item(id: Int, type: MediaType, title: String, rating: Double, added: Long, status: WatchStatus) =
        LibraryItem(
            entry = LibraryEntry(
                MediaKey(type, id),
                status,
                id % 2 == 0,
                Instant.ofEpochSecond(added),
                null,
                null,
                null,
            ),
            media = testMedia(id, type, voteAverage = rating).copy(title = title),
        )

    private val items = listOf(
        item(1, MediaType.MOVIE, "Ёлки", 6.0, added = 30, WatchStatus.WANT),
        item(2, MediaType.TV, "Аркейн", 9.0, added = 10, WatchStatus.WANT),
        item(3, MediaType.MOVIE, "Бойцовский клуб", 8.4, added = 20, WatchStatus.WANT),
        item(4, MediaType.MOVIE, "Дюна", 8.0, added = 40, WatchStatus.WATCHED),
    )

    @Test
    fun `tab is sorted by date, title or rating`() {
        fun ids(sort: LibrarySort) = items.forTab(LibraryTab.WANT, MediaFilter.ALL, sort).map { it.media.tmdbId }

        assertEquals(listOf(1, 3, 2), ids(LibrarySort.ADDED))
        assertEquals("Russian alphabet: А, Б, Ё", listOf(2, 3, 1), ids(LibrarySort.TITLE))
        assertEquals(listOf(2, 3, 1), ids(LibrarySort.RATING))
    }

    @Test
    fun `type filter applies to the list and the counters`() {
        assertEquals(
            listOf(1, 3),
            items.forTab(LibraryTab.WANT, MediaFilter.MOVIES, LibrarySort.ADDED).map {
                it.media.tmdbId
            },
        )
        assertEquals(
            mapOf(LibraryTab.WANT to 3, LibraryTab.WATCHED to 1, LibraryTab.FAVORITES to 2),
            items.tabCounts(MediaFilter.ALL),
        )
        assertEquals(
            mapOf(LibraryTab.WANT to 1, LibraryTab.WATCHED to 0, LibraryTab.FAVORITES to 1),
            items.tabCounts(MediaFilter.SERIES),
        )
    }
}

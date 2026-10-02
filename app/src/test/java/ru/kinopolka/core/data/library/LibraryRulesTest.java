package ru.kinopolka.core.data.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.TestData.testMedia;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;

/** Status rule (docs/PLAN.md, section 2) and list building of «Моя полка» (F-11, F-12). */
public class LibraryRulesTest {

    private final MediaKey key = new MediaKey(MediaType.MOVIE, 550);
    private final Instant t1 = Instant.ofEpochSecond(1_000);
    private final Instant t2 = Instant.ofEpochSecond(2_000);

    @Test
    public void watchedReplacesWant() {
        LibraryEntry want = LibraryRules.withWatchStatus(null, key, WatchStatus.WANT, t1);
        LibraryEntry watched = LibraryRules.withWatchStatus(want, key, WatchStatus.WATCHED, t2);

        assertEquals(WatchStatus.WANT, want.watchStatus());
        assertNull(want.watchedAt());
        assertEquals(WatchStatus.WATCHED, watched.watchStatus());
        assertEquals(t2, watched.watchedAt());
        assertEquals(t2, watched.addedAt());
        assertFalse(LibraryTab.WANT.contains(watched));
        assertTrue(LibraryTab.WATCHED.contains(watched));
    }

    @Test
    public void favoriteIsIndependentOfTheStatus() {
        LibraryEntry entry = LibraryRules.withWatchStatus(
                LibraryRules.withFavorite(null, key, true, t1), key, WatchStatus.WATCHED, t2);

        assertTrue(entry.favorite());
        assertTrue(LibraryTab.FAVORITES.contains(entry));
        assertTrue(LibraryTab.WATCHED.contains(entry));

        LibraryEntry cleared = LibraryRules.withWatchStatus(entry, key, WatchStatus.NONE, t2);
        assertTrue("favorite stays", cleared.favorite());
        assertFalse(cleared.isEmpty());
    }

    @Test
    public void entryWithoutStatusAndFavoriteIsEmpty() {
        LibraryEntry entry = LibraryRules.withWatchStatus(
                LibraryRules.withWatchStatus(null, key, WatchStatus.WANT, t1), key, WatchStatus.NONE, t2);

        assertTrue(entry.isEmpty());
        assertTrue(LibraryRules.withFavorite(LibraryRules.withFavorite(null, key, true, t1), key, false, t2).isEmpty());
    }

    @Test
    public void sameStatusAgainKeepsTheEntryUnchanged() {
        LibraryEntry want = LibraryRules.withWatchStatus(null, key, WatchStatus.WANT, t1);

        assertEquals(want, LibraryRules.withWatchStatus(want, key, WatchStatus.WANT, t2));
    }

    @Test
    public void removalFromATabClearsOnlyThatTab() {
        LibraryEntry entry = new LibraryEntry(key, WatchStatus.WATCHED, true, t1, t1, 8, "/p.jpg");

        LibraryEntry fromWatched = LibraryRules.removedFrom(entry, LibraryTab.WATCHED);
        assertEquals(WatchStatus.NONE, fromWatched.watchStatus());
        assertNull(fromWatched.watchedAt());
        assertTrue(fromWatched.favorite());

        LibraryEntry fromFavorites = LibraryRules.removedFrom(entry, LibraryTab.FAVORITES);
        assertEquals(WatchStatus.WATCHED, fromFavorites.watchStatus());
        assertFalse(fromFavorites.favorite());
    }

    private static LibraryItem item(int id, MediaType type, String title, double rating, long added,
            WatchStatus status) {
        return new LibraryItem(
                new LibraryEntry(new MediaKey(type, id), status, id % 2 == 0, Instant.ofEpochSecond(added), null,
                        null, null),
                testMedia(id, type, 1.0, rating, null).withTitle(title));
    }

    private final List<LibraryItem> items = List.of(
            item(1, MediaType.MOVIE, "Ёлки", 6.0, 30, WatchStatus.WANT),
            item(2, MediaType.TV, "Аркейн", 9.0, 10, WatchStatus.WANT),
            item(3, MediaType.MOVIE, "Бойцовский клуб", 8.4, 20, WatchStatus.WANT),
            item(4, MediaType.MOVIE, "Дюна", 8.0, 40, WatchStatus.WATCHED));

    private List<Integer> ids(LibraryTab tab, MediaFilter filter, LibrarySort sort) {
        return LibraryRules.forTab(items, tab, filter, sort).stream().map(it -> it.media().tmdbId()).toList();
    }

    @Test
    public void tabIsSortedByDateTitleOrRating() {
        assertEquals(List.of(1, 3, 2), ids(LibraryTab.WANT, MediaFilter.ALL, LibrarySort.ADDED));
        assertEquals("Russian alphabet: А, Б, Ё", List.of(2, 3, 1), ids(LibraryTab.WANT, MediaFilter.ALL,
                LibrarySort.TITLE));
        assertEquals(List.of(2, 3, 1), ids(LibraryTab.WANT, MediaFilter.ALL, LibrarySort.RATING));
    }

    @Test
    public void typeFilterAppliesToTheListAndTheCounters() {
        assertEquals(List.of(1, 3), ids(LibraryTab.WANT, MediaFilter.MOVIES, LibrarySort.ADDED));
        assertEquals(Map.of(LibraryTab.WANT, 3, LibraryTab.WATCHED, 1, LibraryTab.FAVORITES, 2),
                LibraryRules.tabCounts(items, MediaFilter.ALL));
        assertEquals(Map.of(LibraryTab.WANT, 1, LibraryTab.WATCHED, 0, LibraryTab.FAVORITES, 1),
                LibraryRules.tabCounts(items, MediaFilter.SERIES));
    }
}

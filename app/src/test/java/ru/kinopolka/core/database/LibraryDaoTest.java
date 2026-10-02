package ru.kinopolka.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.database.dao.LibraryDao;
import ru.kinopolka.core.database.dao.SearchHistoryDao;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.database.entity.SearchHistoryEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;

/** Library and search history tables. */
@RunWith(AndroidJUnit4.class)
public class LibraryDaoTest extends DatabaseTest {

    private static LibraryEntryEntity entry(int id, WatchStatus status, boolean favorite) {
        return new LibraryEntryEntity(MediaType.MOVIE, id, status, favorite, id, null, null, null);
    }

    private static List<Integer> ids(List<LibraryEntryEntity> entries) {
        return entries.stream().map(it -> it.tmdbId).toList();
    }

    @Test
    public void entriesAreSelectedByStatusAndFavoriteFlag() {
        database.mediaDao().upsert(List.of(media(1), media(2), media(3)));
        LibraryDao dao = database.libraryDao();
        dao.upsert(entry(1, WatchStatus.WANT, false));
        dao.upsert(entry(2, WatchStatus.WANT, true));
        dao.upsert(entry(3, WatchStatus.WATCHED, true));

        assertEquals(List.of(2, 1), ids(dao.getByStatus(WatchStatus.WANT)));
        assertEquals(List.of(3), ids(dao.getByStatus(WatchStatus.WATCHED)));
        assertEquals(List.of(3, 2), ids(dao.getFavorites()));
        assertEquals(List.of(3, 2, 1), ids(awaitValue(dao.observeAll())));
        assertEquals(List.of(3, 2, 1), ids(dao.getAll()));
    }

    @Test
    public void statusChangeReplacesTheEntryAndDeleteRemovesIt() {
        database.mediaDao().upsert(List.of(media(1)));
        LibraryDao dao = database.libraryDao();
        dao.upsert(entry(1, WatchStatus.WANT, false));

        dao.upsert(new LibraryEntryEntity(MediaType.MOVIE, 1, WatchStatus.WATCHED, false, 1L, 50L, null, null));
        assertEquals(WatchStatus.WATCHED,
                awaitValue(dao.observeEntry(MediaType.MOVIE, 1), Objects::nonNull).watchStatus);
        assertEquals(List.of(), dao.getByStatus(WatchStatus.WANT));

        dao.updatePosterPath(MediaType.MOVIE, 1, "posters/movie_1.jpg");
        assertEquals(List.of("posters/movie_1.jpg"), dao.posterPaths());

        dao.delete(MediaType.MOVIE, 1);
        assertNull(dao.get(MediaType.MOVIE, 1));
    }

    @Test
    public void searchHistoryKeepsTheMostRecentQueries() {
        SearchHistoryDao dao = database.searchHistoryDao();
        for (int i = 1; i <= 12; i++) {
            dao.upsert(new SearchHistoryEntity("запрос " + i, i));
        }

        dao.trimTo(10);

        List<SearchHistoryEntity> recent = dao.getRecent(20);
        assertEquals(10, recent.size());
        assertEquals("запрос 12", recent.get(0).query);
        assertEquals("запрос 3", recent.get(recent.size() - 1).query);

        dao.clear();
        assertEquals(List.of(), dao.getRecent(10));
    }
}

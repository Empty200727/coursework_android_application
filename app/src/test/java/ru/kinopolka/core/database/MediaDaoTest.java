package ru.kinopolka.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.database.dao.MediaDao;
import ru.kinopolka.core.database.entity.CastMemberEntity;
import ru.kinopolka.core.database.entity.FeedItemEntity;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.database.entity.RelatedMediaEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;
import ru.kinopolka.core.model.WatchStatus;

@RunWith(AndroidJUnit4.class)
public class MediaDaoTest extends DatabaseTest {

    private MediaDao mediaDao() {
        return database.mediaDao();
    }

    @Test
    public void movieAndSeriesWithTheSameIdAreDifferentRows() {
        mediaDao().upsert(List.of(media(1396, MediaType.MOVIE, "Фильм", 1L, null, null),
                media(1396, MediaType.TV, "Сериал", 1L, null, null)));

        assertEquals("Фильм", mediaDao().get(MediaType.MOVIE, 1396).title);
        assertEquals("Сериал", mediaDao().get(MediaType.TV, 1396).title);
        assertEquals(2, mediaDao().count());
    }

    @Test
    public void listDataDoesNotEraseLoadedDetails() {
        mediaDao().upsert(List.of(media(550, MediaType.MOVIE, "Fight Club", 1_000L, 139, 1_000L)));

        mediaDao().upsertSummaries(List.of(media(550, MediaType.MOVIE, "Бойцовский клуб", 2_000L, null, null),
                media(551)));

        MediaEntity updated = Objects.requireNonNull(mediaDao().get(MediaType.MOVIE, 550));
        assertEquals("Бойцовский клуб", updated.title);
        assertEquals(2_000L, updated.cachedAt);
        assertEquals(Integer.valueOf(139), updated.runtime);
        assertEquals(Long.valueOf(1_000L), updated.detailsCachedAt);
        assertNotNull(mediaDao().get(MediaType.MOVIE, 551));
    }

    @Test
    public void observeEmitsTheCurrentRow() {
        assertNull(awaitValue(mediaDao().observe(MediaType.MOVIE, 550)));

        mediaDao().upsert(List.of(media(550)));

        assertEquals("Фильм 550", awaitValue(mediaDao().observe(MediaType.MOVIE, 550), Objects::nonNull).title);
    }

    @Test
    public void genresOfATitleAreReplaced() {
        mediaDao().upsert(List.of(media(550)));
        mediaDao().replaceGenres(MediaType.MOVIE, 550, List.of(18, 53, 18));
        assertEquals(Set.of(18, 53), new HashSet<>(awaitValue(mediaDao().observeGenreIds(MediaType.MOVIE, 550))));

        mediaDao().replaceGenres(MediaType.MOVIE, 550, List.of(35));
        assertEquals(List.of(35), mediaDao().getGenreIds(MediaType.MOVIE, 550));
    }

    @Test
    public void cleanupRemovesOldTitlesWithTheirCacheButKeepsTheLibrary() {
        mediaDao().upsert(List.of(media(1, 100L), media(2, 100L), media(3, 5_000L)));
        mediaDao().replaceGenres(MediaType.MOVIE, 1, List.of(18));
        database.castDao().insertCast(List.of(new CastMemberEntity(MediaType.MOVIE, 1, 10, "Актёр", null, null, 0)));
        database.feedDao().insertItems(List.of(new FeedItemEntity("trending", 0, MediaType.MOVIE, 1, 100L)));
        database.relatedMediaDao().insertRelated(
                List.of(new RelatedMediaEntity(MediaType.MOVIE, 3, RelatedKind.SIMILAR, MediaType.MOVIE, 1, 0)));
        database.libraryDao().upsert(
                new LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WANT, false, 100L, null, null, null));

        int deleted = mediaDao().deleteUnusedCachedBefore(1_000L);

        assertEquals(1, deleted);
        assertNull(mediaDao().get(MediaType.MOVIE, 1));
        assertNotNull("library title is kept", mediaDao().get(MediaType.MOVIE, 2));
        assertNotNull("fresh title is kept", mediaDao().get(MediaType.MOVIE, 3));
        assertTrue(mediaDao().getGenreIds(MediaType.MOVIE, 1).isEmpty());
        assertTrue(database.castDao().getCast(MediaType.MOVIE, 1).isEmpty());
        assertTrue(database.feedDao().getFeed("trending").isEmpty());
        assertTrue(database.relatedMediaDao().getRelated(MediaType.MOVIE, 3, RelatedKind.SIMILAR).isEmpty());
        assertNotNull(database.libraryDao().get(MediaType.MOVIE, 2));
        assertEquals(1, awaitValue(mediaDao().observeLibraryMedia()).size());
    }

    @Test
    public void aTitleInTheLibraryCannotBeDeleted() {
        mediaDao().upsert(List.of(media(2)));
        database.libraryDao().upsert(
                new LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WATCHED, true, 100L, 200L, 9, null));

        assertThrows(RuntimeException.class,
                () -> database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM media WHERE tmdb_id = 2"));
        assertNotNull(mediaDao().get(MediaType.MOVIE, 2));
    }
}

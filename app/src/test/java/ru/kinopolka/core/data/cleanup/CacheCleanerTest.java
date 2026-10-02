package ru.kinopolka.core.data.cleanup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.database.DatabaseTest;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.FakePosterStorage;
import ru.kinopolka.testing.TestTimeProvider;

/** The daily cleanup removes old cache but never the library (docs/PLAN.md, section 6). */
@RunWith(AndroidJUnit4.class)
public class CacheCleanerTest extends DatabaseTest {

    private final TestTimeProvider clock = new TestTimeProvider();
    private final FakePosterStorage posters = new FakePosterStorage();

    private CacheCleaner cleaner() {
        return new CacheCleaner(database.mediaDao(), database.libraryDao(), posters, clock);
    }

    @Test
    public void oldCacheIsRemovedLibraryAndFreshCacheStay() {
        long old = clock.nowMillis();
        database.mediaDao().upsert(List.of(media(1, old), media(2, old)));
        database.libraryDao().upsert(new LibraryEntryEntity(MediaType.MOVIE, 2, WatchStatus.WANT, false, old, null,
                null, "posters/movie_2.jpg"));
        clock.advanceBy(TimeUnit.DAYS.toMillis(31));
        database.mediaDao().upsert(List.of(media(3, clock.nowMillis())));
        posters.files.addAll(Set.of("posters/movie_2.jpg", "posters/movie_9.jpg"));

        CacheCleaner.Result result = cleaner().clean();

        assertEquals(new CacheCleaner.Result(1, 1), result);
        assertNull(database.mediaDao().get(MediaType.MOVIE, 1));
        assertNotNull("library title", database.mediaDao().get(MediaType.MOVIE, 2));
        assertNotNull("fresh title", database.mediaDao().get(MediaType.MOVIE, 3));
        assertEquals(Set.of("posters/movie_2.jpg"), posters.files);
    }

    @Test
    public void cacheYoungerThan30DaysIsKept() {
        database.mediaDao().upsert(List.of(media(1, clock.nowMillis())));
        clock.advanceBy(TimeUnit.DAYS.toMillis(29));

        assertEquals(0, cleaner().clean().removedTitles());
    }
}

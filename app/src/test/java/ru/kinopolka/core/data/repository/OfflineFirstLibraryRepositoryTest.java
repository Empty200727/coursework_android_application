package ru.kinopolka.core.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.database.DatabaseTest;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.FakePosterStorage;
import ru.kinopolka.testing.TestTimeProvider;

@RunWith(AndroidJUnit4.class)
public class OfflineFirstLibraryRepositoryTest extends DatabaseTest {

    private final FakePosterStorage posters = new FakePosterStorage();
    private final TestTimeProvider clock = new TestTimeProvider();
    private final MediaKey key = new MediaKey(MediaType.MOVIE, 550);

    private OfflineFirstLibraryRepository repository() {
        database.mediaDao().upsert(List.of(media(550)));
        return new OfflineFirstLibraryRepository(database, database.libraryDao(), database.mediaDao(), posters, clock);
    }

    private LibraryEntry entry(OfflineFirstLibraryRepository repository) {
        return database.libraryDao().get(key.mediaType(), key.tmdbId()) == null
                ? null : awaitValue(repository.observeEntry(key), Objects::nonNull);
    }

    @Test
    public void addingATitleSavesItsPoster() {
        OfflineFirstLibraryRepository repository = repository();

        repository.setWatchStatus(key, WatchStatus.WANT);

        LibraryEntry entry = entry(repository);
        assertEquals(WatchStatus.WANT, entry.watchStatus());
        assertEquals("posters/movie_550.jpg", entry.localPosterPath());
        assertEquals(List.of(key), awaitValue(repository.observeItems(), items -> !items.isEmpty()).stream()
                .map(it -> it.media().key()).toList());
    }

    @Test
    public void wantAndWatchedExcludeEachOtherFavoriteIsIndependent() {
        OfflineFirstLibraryRepository repository = repository();

        repository.setWatchStatus(key, WatchStatus.WANT);
        repository.setFavorite(key, true);
        repository.setWatchStatus(key, WatchStatus.WATCHED);

        LibraryEntry entry = entry(repository);
        assertEquals(WatchStatus.WATCHED, entry.watchStatus());
        assertTrue(entry.favorite());
        assertEquals(1, awaitValue(repository.observeItems()).size());
    }

    @Test
    public void entryWithoutStatusAndFavoriteIsDeletedWithItsPoster() {
        OfflineFirstLibraryRepository repository = repository();
        repository.setWatchStatus(key, WatchStatus.WANT);
        repository.setFavorite(key, true);

        repository.setFavorite(key, false);
        assertNotNull(entry(repository));

        repository.setWatchStatus(key, WatchStatus.NONE);
        assertNull(awaitValue(repository.observeEntry(key)));
        assertTrue("poster file removed", posters.files.isEmpty());
    }

    @Test
    public void removalCanBeUndoneAndKeepsThePosterUntilReleased() {
        OfflineFirstLibraryRepository repository = repository();
        repository.setWatchStatus(key, WatchStatus.WANT);

        LibraryEntry previous = Objects.requireNonNull(repository.removeFromTab(key, LibraryTab.WANT));
        assertNull(awaitValue(repository.observeEntry(key)));
        assertEquals(Set.of("posters/movie_550.jpg"), posters.files);

        repository.restore(previous);
        assertEquals(previous, entry(repository));

        repository.removeFromTab(key, LibraryTab.WANT);
        repository.releasePoster(previous);
        assertTrue(posters.files.isEmpty());
    }

    @Test
    public void restoredEntryForgetsAPosterThatIsGone() {
        OfflineFirstLibraryRepository repository = repository();
        repository.setWatchStatus(key, WatchStatus.WANT);
        LibraryEntry previous = Objects.requireNonNull(repository.removeFromTab(key, LibraryTab.WANT));
        posters.files.clear();

        repository.restore(previous);

        assertEquals("saved again", "posters/movie_550.jpg", entry(repository).localPosterPath());
    }

    @Test
    public void removingATitleThatIsNotInTheLibraryFails() {
        OfflineFirstLibraryRepository repository = repository();

        assertThrows(IllegalArgumentException.class, () -> repository.removeFromTab(key, LibraryTab.WANT));
    }

    @Test
    public void releasingDoesNotDeleteThePosterOfATitleStillInTheLibrary() {
        OfflineFirstLibraryRepository repository = repository();
        repository.setWatchStatus(key, WatchStatus.WATCHED);
        repository.setFavorite(key, true);

        LibraryEntry previous = Objects.requireNonNull(repository.removeFromTab(key, LibraryTab.FAVORITES));
        repository.releasePoster(previous);

        assertEquals(Set.of("posters/movie_550.jpg"), posters.files);
        assertEquals(WatchStatus.WATCHED, entry(repository).watchStatus());
    }

    @Test
    public void posterIsRetriedLaterWhenSavingFailed() {
        OfflineFirstLibraryRepository repository = repository();
        posters.failSaving = true;
        repository.setWatchStatus(key, WatchStatus.WANT);
        assertNull(entry(repository).localPosterPath());

        posters.failSaving = false;
        repository.setFavorite(key, true);

        assertEquals("posters/movie_550.jpg", entry(repository).localPosterPath());
    }

    @Test
    public void libraryTitlesAreKeptByTheCacheCleanup() {
        OfflineFirstLibraryRepository repository = repository();
        repository.setWatchStatus(key, WatchStatus.WANT);

        database.mediaDao().deleteUnusedCachedBefore(Long.MAX_VALUE);

        assertEquals(1, awaitValue(repository.observeItems()).size());
    }
}

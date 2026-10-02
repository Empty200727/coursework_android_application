package ru.kinopolka.core.data.library;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;

/** N-01: posters of library titles are stored as files. */
public class FilePosterStorageTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private final MockWebServer server = new MockWebServer();
    private File directory;
    private FilePosterStorage storage;

    @Before
    public void setUp() throws IOException {
        server.start();
        directory = new File(folder.getRoot(), "posters");
        storage = new FilePosterStorage(directory, new OkHttpClient(), server.url("/t/p/").toString());
    }

    @After
    public void tearDown() {
        server.close();
    }

    @Test
    public void posterIsDownloadedInW342IntoThePostersFolder() throws Exception {
        server.enqueue(new MockResponse.Builder().body(new Buffer().write(new byte[] {1, 2, 3})).build());

        String path = storage.save(new MediaKey(MediaType.TV, 1396), "/poster.jpg");

        assertEquals("/t/p/w342/poster.jpg", server.takeRequest().getUrl().encodedPath());
        assertNotNull(path);
        File file = new File(path);
        assertEquals(new File(directory, "tv_1396.jpg").getAbsolutePath(), file.getAbsolutePath());
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file.toPath()));
        assertTrue(storage.exists(file.getPath()));
    }

    @Test
    public void failedDownloadLeavesNoFile() {
        server.enqueue(new MockResponse.Builder().code(404).build());

        assertNull(storage.save(new MediaKey(MediaType.MOVIE, 1), "/missing.jpg"));
        File[] files = directory.listFiles();
        assertTrue(files == null || files.length == 0);
    }

    @Test
    public void brokenConnectionLeavesNoPartialFile() throws IOException {
        server.close();

        assertNull(storage.save(new MediaKey(MediaType.MOVIE, 1), "/poster.jpg"));
        File[] files = directory.listFiles();
        assertTrue(files == null || files.length == 0);
    }

    @Test
    public void filesNotKeptByTheLibraryAreDeleted() throws IOException {
        assertTrue(directory.mkdirs());
        File kept = new File(directory, "movie_1.jpg");
        File orphan = new File(directory, "movie_2.jpg");
        Files.writeString(kept.toPath(), "a");
        Files.writeString(orphan.toPath(), "b");

        assertEquals(1, storage.deleteAllExcept(Set.of(kept.getPath())));
        assertTrue(kept.exists());
        assertFalse(orphan.exists());

        storage.delete(kept.getPath());
        assertFalse(kept.exists());
    }

    @Test
    public void filesOutsideTheFolderAreNeverDeleted() throws IOException {
        File outside = folder.newFile("other.jpg");

        storage.delete(outside.getPath());

        assertTrue(outside.exists());
        assertEquals("missing folder has nothing to delete", 0, storage.deleteAllExcept(Set.of()));
    }
}

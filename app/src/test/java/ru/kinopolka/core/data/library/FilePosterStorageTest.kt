package ru.kinopolka.core.data.library

import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType

/** N-01: posters of library titles are stored as files. */
class FilePosterStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var directory: File
    private lateinit var storage: FilePosterStorage

    @Before
    fun setUp() {
        server.start()
        directory = File(folder.root, "posters")
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun kotlinx.coroutines.test.TestScope.createStorage() = FilePosterStorage(
        directory = directory,
        client = OkHttpClient(),
        imageBaseUrl = server.url("/t/p/").toString(),
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `poster is downloaded in w342 into the posters folder`() = runTest {
        storage = createStorage()
        server.enqueue(MockResponse.Builder().body(Buffer().write(byteArrayOf(1, 2, 3))).build())

        val path = storage.save(MediaKey(MediaType.TV, 1396), "/poster.jpg")

        assertEquals("/t/p/w342/poster.jpg", server.takeRequest().url.encodedPath)
        val file = File(requireNotNull(path))
        assertEquals(File(directory, "tv_1396.jpg").absolutePath, file.absolutePath)
        assertEquals(listOf<Byte>(1, 2, 3), file.readBytes().toList())
        assertTrue(storage.exists(file.path))
    }

    @Test
    fun `failed download leaves no file`() = runTest {
        storage = createStorage()
        server.enqueue(MockResponse.Builder().code(404).build())

        assertNull(storage.save(MediaKey(MediaType.MOVIE, 1), "/missing.jpg"))
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `files not kept by the library are deleted`() = runTest {
        storage = createStorage()
        directory.mkdirs()
        val kept = File(directory, "movie_1.jpg").apply { writeText("a") }
        val orphan = File(directory, "movie_2.jpg").apply { writeText("b") }

        assertEquals(1, storage.deleteAllExcept(setOf(kept.path)))
        assertTrue(kept.exists())
        assertFalse(orphan.exists())

        storage.delete(kept.path)
        assertFalse(kept.exists())
    }

    @Test
    fun `files outside the folder are never deleted`() = runTest {
        storage = createStorage()
        val outside = folder.newFile("other.jpg")

        storage.delete(outside.path)

        assertTrue(outside.exists())
    }
}

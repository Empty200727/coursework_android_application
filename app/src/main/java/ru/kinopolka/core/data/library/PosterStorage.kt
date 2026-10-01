package ru.kinopolka.core.data.library

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.network.ImageSize
import ru.kinopolka.core.network.tmdbImageUrl

/**
 * Posters of library titles in internal storage (`filesDir/posters`): the image cache of Coil
 * evicts old files, these stay available offline (N-01).
 */
interface PosterStorage {
    /** Downloads the poster and returns the file path, or `null` if it could not be saved. */
    suspend fun save(key: MediaKey, posterPath: String): String?

    fun exists(path: String): Boolean

    suspend fun delete(path: String)

    /** Removes saved posters not in [keep]; returns the number of deleted files. */
    suspend fun deleteAllExcept(keep: Set<String>): Int
}

class FilePosterStorage(
    private val directory: File,
    private val client: OkHttpClient,
    private val imageBaseUrl: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PosterStorage {

    override suspend fun save(key: MediaKey, posterPath: String): String? = withContext(ioDispatcher) {
        val url = tmdbImageUrl(posterPath, ImageSize.POSTER_LIST, imageBaseUrl) ?: return@withContext null
        val target = File(directory, "${key.mediaType.key}_${key.tmdbId}.jpg")
        val partial = File(directory, target.name + ".part")
        try {
            directory.mkdirs()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful) return@withContext null
                partial.outputStream().use { output -> body.byteStream().copyTo(output) }
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target.absolutePath
        } catch (ignored: IOException) {
            // No poster file: the library shows the network image and saving is retried later.
            partial.delete()
            null
        }
    }

    override fun exists(path: String): Boolean = File(path).isFile

    override suspend fun delete(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            // Only files of this storage may be removed.
            if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
        }
    }

    override suspend fun deleteAllExcept(keep: Set<String>): Int = withContext(ioDispatcher) {
        val kept = keep.map { File(it).canonicalPath }.toSet()
        directory.listFiles().orEmpty()
            .filter { it.isFile && it.canonicalPath !in kept }
            .count { it.delete() }
    }
}

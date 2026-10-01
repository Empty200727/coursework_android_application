package ru.kinopolka.core.data.cleanup

import javax.inject.Inject
import ru.kinopolka.core.data.CachePolicy
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.library.PosterStorage
import ru.kinopolka.core.database.dao.LibraryDao
import ru.kinopolka.core.database.dao.MediaDao

/**
 * Daily cleanup (docs/PLAN.md, section 6): cached titles older than 30 days are removed unless
 * they are in the library; saved posters of titles no longer in the library are deleted.
 */
class CacheCleaner @Inject constructor(
    private val mediaDao: MediaDao,
    private val libraryDao: LibraryDao,
    private val posterStorage: PosterStorage,
    private val timeProvider: TimeProvider,
) {
    data class Result(val removedTitles: Int, val removedPosters: Int)

    suspend fun clean(): Result {
        val threshold = timeProvider.nowMillis() - CachePolicy.CLEANUP_AGE.inWholeMilliseconds
        val removedTitles = mediaDao.deleteUnusedCachedBefore(threshold)
        val removedPosters = posterStorage.deleteAllExcept(libraryDao.posterPaths().toSet())
        return Result(removedTitles, removedPosters)
    }
}

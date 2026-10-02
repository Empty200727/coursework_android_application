package ru.kinopolka.core.data.cleanup;

import androidx.annotation.WorkerThread;
import java.util.HashSet;
import javax.inject.Inject;
import ru.kinopolka.core.data.CachePolicy;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.library.PosterStorage;
import ru.kinopolka.core.database.dao.LibraryDao;
import ru.kinopolka.core.database.dao.MediaDao;

/**
 * Daily cleanup (docs/PLAN.md, section 6): cached titles older than 30 days are removed unless
 * they are in the library; saved posters of titles no longer in the library are deleted.
 */
public final class CacheCleaner {

    public record Result(int removedTitles, int removedPosters) {
    }

    private final MediaDao mediaDao;
    private final LibraryDao libraryDao;
    private final PosterStorage posterStorage;
    private final TimeProvider timeProvider;

    @Inject
    public CacheCleaner(MediaDao mediaDao, LibraryDao libraryDao, PosterStorage posterStorage,
            TimeProvider timeProvider) {
        this.mediaDao = mediaDao;
        this.libraryDao = libraryDao;
        this.posterStorage = posterStorage;
        this.timeProvider = timeProvider;
    }

    @WorkerThread
    public Result clean() {
        long threshold = timeProvider.nowMillis() - CachePolicy.CLEANUP_AGE;
        int removedTitles = mediaDao.deleteUnusedCachedBefore(threshold);
        int removedPosters = posterStorage.deleteAllExcept(new HashSet<>(libraryDao.posterPaths()));
        return new Result(removedTitles, removedPosters);
    }
}

package ru.kinopolka.core.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingData;
import java.util.List;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.Shelf;

/** Catalog of titles: home shelves cached in Room, search and genre lists paged from TMDB. */
public interface MediaRepository {

    /** Titles of the shelf from the local database; empty until the first successful refresh. */
    LiveData<List<Media>> observeShelf(Shelf shelf);

    /** Loads the shelf from TMDB if it was never loaded or is older than 6 hours (N-03), or if {@code force}. */
    @WorkerThread
    RefreshResult refreshShelf(Shelf shelf, boolean force);

    /** Search results (F-01…F-03); not cached in Room. */
    LiveData<PagingData<Media>> search(String query, MediaFilter filter);

    /** Full list of a genre (F-06); not cached in Room. */
    LiveData<PagingData<Media>> genreMedia(Genre genre, MediaFilter filter, MediaSort sort);
}

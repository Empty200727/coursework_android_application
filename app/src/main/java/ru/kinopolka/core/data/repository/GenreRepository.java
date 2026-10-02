package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;

/** Unified genre catalog (F-04), stored in Room and refreshed from TMDB once in 7 days. */
public interface GenreRepository {

    /** Genres from the local database; empty until the first successful refresh. */
    LiveData<List<Genre>> observeGenres();

    default LiveData<List<Genre>> observeGenres(MediaFilter filter) {
        return Transformations.map(observeGenres(), genres -> {
            List<Genre> result = new ArrayList<>();
            for (Genre genre : genres) {
                if (genre.matches(filter)) {
                    result.add(genre);
                }
            }
            return result;
        });
    }

    @Nullable
    @WorkerThread
    Genre getGenre(String key);

    /** TMDB genre names by type and id, for the genres of search results (docs/PLAN.md, section 7). */
    LiveData<Map<MediaType, Map<Integer, String>>> observeGenreNames();

    /**
     * Loads genres from TMDB if the cache is missing or older than 7 days, or if {@code force}.
     * A failed refresh keeps the cached genres.
     */
    @WorkerThread
    RefreshResult refresh(boolean force);
}

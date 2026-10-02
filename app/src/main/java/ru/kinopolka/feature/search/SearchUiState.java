package ru.kinopolka.feature.search;

import androidx.annotation.Nullable;
import java.util.Map;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;

/**
 * State of the search screen.
 *
 * @param genreNames genre names from the local catalog for result rows
 * @param resultsQuery query of the results on screen; differs from {@code query} while the debounce runs
 */
public record SearchUiState(
        String query,
        MediaFilter filter,
        Map<MediaType, Map<Integer, String>> genreNames,
        boolean offline,
        @Nullable String resultsQuery) {

    /** F-01: a request is sent only from two characters. */
    public static final int MIN_QUERY_LENGTH = 2;
    public static final long DEBOUNCE_MILLIS = 400L;

    public boolean isQueryTooShort() {
        return query.trim().length() < MIN_QUERY_LENGTH;
    }

    /** The query was changed and its results are not requested yet. */
    public boolean isPending() {
        return !isQueryTooShort() && !query.trim().equals(resultsQuery);
    }
}

package ru.kinopolka.feature.home;

import androidx.annotation.Nullable;
import java.util.List;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.Shelf;

/**
 * State of the home screen.
 *
 * @param error last refresh error; cached shelves stay visible
 */
public record HomeUiState(
        MediaFilter filter,
        List<ShelfContent> shelves,
        boolean refreshing,
        @Nullable DataError error,
        boolean offline) {

    public static final HomeUiState INITIAL = new HomeUiState(MediaFilter.ALL, List.of(), true, null, false);

    public record ShelfContent(Shelf shelf, List<Media> items) {
    }

    /** Screen state variants (docs/PLAN.md, section 7): loading, data, empty, error. */
    public enum Content {
        LOADING,
        DATA,
        EMPTY,
        ERROR
    }

    public Content content() {
        for (ShelfContent shelf : shelves) {
            if (!shelf.items().isEmpty()) {
                return Content.DATA;
            }
        }
        if (refreshing) {
            return Content.LOADING;
        }
        return error != null ? Content.ERROR : Content.EMPTY;
    }
}

package ru.kinopolka.feature.details;

import androidx.annotation.Nullable;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;

public record DetailsUiState(
        MediaKey key,
        @Nullable MediaDetails details,
        @Nullable LibraryEntry entry,
        boolean refreshing,
        @Nullable DataError error,
        boolean offline) {

    public enum Content {
        LOADING,
        DATA,
        ERROR
    }

    public Content content() {
        if (details != null) {
            return Content.DATA;
        }
        return refreshing ? Content.LOADING : Content.ERROR;
    }

    public WatchStatus watchStatus() {
        return entry != null ? entry.watchStatus() : WatchStatus.NONE;
    }

    public boolean favorite() {
        return entry != null && entry.favorite();
    }
}

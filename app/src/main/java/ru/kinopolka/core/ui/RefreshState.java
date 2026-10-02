package ru.kinopolka.core.ui;

import androidx.annotation.Nullable;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;

/** Progress of a background refresh; the last error is kept while cached data stays visible. */
public record RefreshState(boolean refreshing, @Nullable DataError error) {

    public static final RefreshState LOADING = new RefreshState(true, null);
    public static final RefreshState OFFLINE = new RefreshState(false, DataError.NO_CONNECTION);

    public static RefreshState finished(RefreshResult result) {
        return new RefreshState(false, result instanceof RefreshResult.Failed failed ? failed.error() : null);
    }
}

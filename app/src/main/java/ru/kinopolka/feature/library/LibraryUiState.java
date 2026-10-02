package ru.kinopolka.feature.library;

import androidx.annotation.Nullable;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaFilter;

public record LibraryUiState(
        LibraryTab tab,
        LibrarySort sort,
        MediaFilter filter,
        List<LibraryItem> items,
        Map<LibraryTab, Integer> counts,
        boolean loading,
        @Nullable PendingUndo pendingUndo) {

    /** A removal that can still be undone from the Snackbar (F-13). */
    public record PendingUndo(LibraryEntry entry, String title, LibraryTab tab) {
    }

    public int count(LibraryTab libraryTab) {
        Integer count = counts.get(libraryTab);
        return count != null ? count : 0;
    }
}

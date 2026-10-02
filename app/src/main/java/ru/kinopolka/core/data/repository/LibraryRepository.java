package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import java.util.List;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;

/** «Моя полка» (F-11…F-13): stored only on the device and available offline (N-01). */
public interface LibraryRepository {

    /** Emits {@code null} when the title is not in the library. */
    LiveData<LibraryEntry> observeEntry(MediaKey key);

    /** All entries with the cached data of their titles. */
    LiveData<List<LibraryItem>> observeItems();

    /** The title must already be stored in {@code media} (it is, once its card was opened). */
    @WorkerThread
    void setWatchStatus(MediaKey key, WatchStatus status);

    @WorkerThread
    void setFavorite(MediaKey key, boolean favorite);

    /**
     * Removes the title from {@code tab} and returns the entry as it was, for «Отменить».
     * The saved poster is kept until {@link #releasePoster}.
     */
    @Nullable
    @WorkerThread
    LibraryEntry removeFromTab(MediaKey key, LibraryTab tab);

    /** Puts back an entry returned by {@link #removeFromTab}. */
    @WorkerThread
    void restore(LibraryEntry entry);

    /** Deletes the saved poster of the entry if the title is no longer in the library. */
    @WorkerThread
    void releasePoster(LibraryEntry entry);
}

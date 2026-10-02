package ru.kinopolka.core.data.library;

import androidx.annotation.Nullable;
import java.util.Set;
import ru.kinopolka.core.model.MediaKey;

/**
 * Posters of library titles in internal storage ({@code filesDir/posters}): the image cache
 * evicts old files, these stay available offline (N-01). Methods block and run on a background thread.
 */
public interface PosterStorage {

    /** Downloads the poster and returns the file path, or {@code null} if it could not be saved. */
    @Nullable
    String save(MediaKey key, String posterPath);

    boolean exists(String path);

    void delete(String path);

    /** Removes saved posters not in {@code keep}; returns the number of deleted files. */
    int deleteAllExcept(Set<String> keep);
}

package ru.kinopolka.testing;

import androidx.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import ru.kinopolka.core.data.library.PosterStorage;
import ru.kinopolka.core.model.MediaKey;

/** Keeps "saved" posters in memory: the path is {@code posters/<type>_<id>.jpg}. */
public final class FakePosterStorage implements PosterStorage {

    public final Set<String> files = new HashSet<>();
    public boolean failSaving;

    @Nullable
    @Override
    public synchronized String save(MediaKey key, String posterPath) {
        if (failSaving) {
            return null;
        }
        String path = "posters/" + key.mediaType().key() + "_" + key.tmdbId() + ".jpg";
        files.add(path);
        return path;
    }

    @Override
    public synchronized boolean exists(String path) {
        return files.contains(path);
    }

    @Override
    public synchronized void delete(String path) {
        files.remove(path);
    }

    @Override
    public synchronized int deleteAllExcept(Set<String> keep) {
        Set<String> removed = new HashSet<>(files);
        removed.removeAll(keep);
        files.removeAll(removed);
        return removed.size();
    }
}

package ru.kinopolka.testing;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaType;

public final class FakeGenreRepository implements GenreRepository {

    public final MutableLiveData<List<Genre>> genres = new MutableLiveData<>(List.of());
    public final MutableLiveData<Map<MediaType, Map<Integer, String>>> names = new MutableLiveData<>(Map.of());

    /** Completed by the test to finish a pending refresh; a fresh repository answers at once. */
    public volatile CompletableFuture<RefreshResult> nextResult =
            CompletableFuture.completedFuture(RefreshResult.SKIPPED);
    public final AtomicInteger refreshCalls = new AtomicInteger();

    @Override
    public LiveData<List<Genre>> observeGenres() {
        return genres;
    }

    @Nullable
    @Override
    public Genre getGenre(String key) {
        for (Genre genre : genres.getValue()) {
            if (genre.key().equals(key)) {
                return genre;
            }
        }
        return null;
    }

    @Override
    public LiveData<Map<MediaType, Map<Integer, String>>> observeGenreNames() {
        return names;
    }

    @Override
    public RefreshResult refresh(boolean force) {
        refreshCalls.incrementAndGet();
        try {
            return nextResult.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}

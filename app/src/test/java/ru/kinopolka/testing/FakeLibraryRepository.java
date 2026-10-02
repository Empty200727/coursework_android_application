package ru.kinopolka.testing;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.data.library.LibraryRules;
import ru.kinopolka.core.data.repository.LibraryRepository;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;

/** In-memory library following the same status rule as the real repository. */
public final class FakeLibraryRepository implements LibraryRepository {

    private final TestTimeProvider clock;
    private final Map<MediaKey, LibraryEntry> entryMap = new LinkedHashMap<>();
    private final Map<MediaKey, Media> mediaMap = new LinkedHashMap<>();
    public final MutableLiveData<Map<MediaKey, LibraryEntry>> entries = new MutableLiveData<>(Map.of());
    /** Cached titles that library entries refer to. */
    public final MutableLiveData<Map<MediaKey, Media>> media = new MutableLiveData<>(Map.of());
    public final List<LibraryEntry> releasedPosters = Collections.synchronizedList(new ArrayList<>());

    public FakeLibraryRepository() {
        this(new TestTimeProvider());
    }

    public FakeLibraryRepository(TestTimeProvider clock) {
        this.clock = clock;
    }

    public synchronized void putMedia(Media item) {
        mediaMap.put(item.key(), item);
        media.postValue(Map.copyOf(mediaMap));
    }

    public synchronized void putEntry(LibraryEntry entry) {
        put(entry.key(), entry);
    }

    @Nullable
    public synchronized LibraryEntry entry(MediaKey key) {
        return entryMap.get(key);
    }

    private synchronized void put(MediaKey key, LibraryEntry entry) {
        if (entry.isEmpty()) {
            entryMap.remove(key);
        } else {
            entryMap.put(key, entry);
        }
        entries.postValue(new LinkedHashMap<>(entryMap));
    }

    private Instant now() {
        return Instant.ofEpochMilli(clock.nowMillis());
    }

    @Override
    public LiveData<LibraryEntry> observeEntry(MediaKey key) {
        return Transformations.map(entries, map -> map.get(key));
    }

    @Override
    public LiveData<List<LibraryItem>> observeItems() {
        MediatorLiveData<List<LibraryItem>> items = new MediatorLiveData<>();
        Runnable update = () -> {
            Map<MediaKey, Media> titles = media.getValue();
            List<LibraryItem> result = new ArrayList<>();
            for (LibraryEntry entry : entries.getValue().values()) {
                Media title = titles.get(entry.key());
                if (title != null) {
                    result.add(new LibraryItem(entry, title));
                }
            }
            items.setValue(result);
        };
        items.addSource(entries, ignored -> update.run());
        items.addSource(media, ignored -> update.run());
        return items;
    }

    @Override
    public synchronized void setWatchStatus(MediaKey key, WatchStatus status) {
        put(key, LibraryRules.withWatchStatus(entryMap.get(key), key, status, now()));
    }

    @Override
    public synchronized void setFavorite(MediaKey key, boolean favorite) {
        put(key, LibraryRules.withFavorite(entryMap.get(key), key, favorite, now()));
    }

    @Nullable
    @Override
    public synchronized LibraryEntry removeFromTab(MediaKey key, LibraryTab tab) {
        LibraryEntry entry = entryMap.get(key);
        if (entry == null) {
            return null;
        }
        put(key, LibraryRules.removedFrom(entry, tab));
        return entry;
    }

    @Override
    public synchronized void restore(LibraryEntry entry) {
        put(entry.key(), entry);
    }

    @Override
    public void releasePoster(LibraryEntry entry) {
        releasedPosters.add(entry);
    }
}

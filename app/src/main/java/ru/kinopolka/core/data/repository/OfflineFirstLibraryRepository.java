package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import javax.inject.Singleton;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.library.LibraryRules;
import ru.kinopolka.core.data.library.PosterStorage;
import ru.kinopolka.core.data.mapper.EntityMappers;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.database.KinopolkaDatabase;
import ru.kinopolka.core.database.dao.LibraryDao;
import ru.kinopolka.core.database.dao.MediaDao;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;

@Singleton
public final class OfflineFirstLibraryRepository implements LibraryRepository {

    private final KinopolkaDatabase database;
    private final LibraryDao libraryDao;
    private final MediaDao mediaDao;
    private final PosterStorage posterStorage;
    private final TimeProvider timeProvider;

    @Inject
    public OfflineFirstLibraryRepository(KinopolkaDatabase database, LibraryDao libraryDao, MediaDao mediaDao,
            PosterStorage posterStorage, TimeProvider timeProvider) {
        this.database = database;
        this.libraryDao = libraryDao;
        this.mediaDao = mediaDao;
        this.posterStorage = posterStorage;
        this.timeProvider = timeProvider;
    }

    @Override
    public LiveData<LibraryEntry> observeEntry(MediaKey key) {
        return Transformations.distinctUntilChanged(Transformations.map(
                libraryDao.observeEntry(key.mediaType(), key.tmdbId()),
                entity -> entity == null ? null : EntityMappers.toLibraryEntry(entity)));
    }

    @Override
    public LiveData<List<LibraryItem>> observeItems() {
        return Transformations.distinctUntilChanged(LiveDataUtils.combine(
                libraryDao.observeAll(), mediaDao.observeLibraryMedia(), (entries, media) -> {
                    Map<MediaKey, MediaEntity> mediaByKey = new HashMap<>();
                    for (MediaEntity entity : media) {
                        mediaByKey.put(new MediaKey(entity.mediaType, entity.tmdbId), entity);
                    }
                    List<LibraryItem> items = new ArrayList<>();
                    for (LibraryEntryEntity entity : entries) {
                        LibraryEntry entry = EntityMappers.toLibraryEntry(entity);
                        MediaEntity cached = mediaByKey.get(entry.key());
                        if (cached != null) {
                            items.add(new LibraryItem(entry, EntityMappers.toMedia(cached)));
                        }
                    }
                    return items;
                }));
    }

    @Override
    public void setWatchStatus(MediaKey key, WatchStatus status) {
        update(key, false, entry -> LibraryRules.withWatchStatus(entry, key, status, now()));
    }

    @Override
    public void setFavorite(MediaKey key, boolean favorite) {
        update(key, false, entry -> LibraryRules.withFavorite(entry, key, favorite, now()));
    }

    @Nullable
    @Override
    public LibraryEntry removeFromTab(MediaKey key, LibraryTab tab) {
        LibraryEntry[] previous = new LibraryEntry[1];
        update(key, true, entry -> {
            if (entry == null) {
                throw new IllegalArgumentException(key + " is not in the library");
            }
            previous[0] = entry;
            return LibraryRules.removedFrom(entry, tab);
        });
        return previous[0];
    }

    @Override
    public void restore(LibraryEntry entry) {
        String poster = entry.localPosterPath();
        String kept = poster != null && posterStorage.exists(poster) ? poster : null;
        update(entry.key(), false, ignored -> entry.withPoster(kept));
    }

    @Override
    public void releasePoster(LibraryEntry entry) {
        String path = entry.localPosterPath();
        if (path != null && libraryDao.get(entry.key().mediaType(), entry.key().tmdbId()) == null) {
            posterStorage.delete(path);
        }
    }

    /**
     * Applies {@code transform} to the stored entry in a transaction. An empty result deletes the
     * entry together with its poster file; a new entry gets its poster saved.
     */
    private void update(MediaKey key, boolean keepPosterOnDelete, UnaryOperator<LibraryEntry> transform) {
        LibraryEntry[] old = new LibraryEntry[1];
        LibraryEntry updated = database.runInTransaction(() -> {
            LibraryEntryEntity stored = libraryDao.get(key.mediaType(), key.tmdbId());
            old[0] = stored == null ? null : EntityMappers.toLibraryEntry(stored);
            LibraryEntry next = transform.apply(old[0]);
            if (next.isEmpty()) {
                libraryDao.delete(key.mediaType(), key.tmdbId());
            } else {
                libraryDao.upsert(EntityMappers.toEntity(next));
            }
            return next;
        });
        if (updated.isEmpty()) {
            if (!keepPosterOnDelete && old[0] != null && old[0].localPosterPath() != null) {
                posterStorage.delete(old[0].localPosterPath());
            }
        } else if (updated.localPosterPath() == null) {
            savePoster(key);
        }
    }

    private void savePoster(MediaKey key) {
        MediaEntity media = mediaDao.get(key.mediaType(), key.tmdbId());
        if (media == null || media.posterPath == null) {
            return;
        }
        String file = posterStorage.save(key, media.posterPath);
        if (file != null) {
            libraryDao.updatePosterPath(key.mediaType(), key.tmdbId(), file);
        }
    }

    private Instant now() {
        return Instant.ofEpochMilli(timeProvider.nowMillis());
    }
}

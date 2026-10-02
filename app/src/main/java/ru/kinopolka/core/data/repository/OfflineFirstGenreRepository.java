package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import ru.kinopolka.core.data.CachePolicy;
import ru.kinopolka.core.data.GenreCatalog;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.mapper.EntityMappers;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.database.dao.GenreDao;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbCalls;

@Singleton
public final class OfflineFirstGenreRepository implements GenreRepository {

    private final TmdbApi api;
    private final GenreDao genreDao;
    private final TimeProvider timeProvider;
    private final Object refreshLock = new Object();

    @Inject
    public OfflineFirstGenreRepository(TmdbApi api, GenreDao genreDao, TimeProvider timeProvider) {
        this.api = api;
        this.genreDao = genreDao;
        this.timeProvider = timeProvider;
    }

    @Override
    public LiveData<List<Genre>> observeGenres() {
        return Transformations.distinctUntilChanged(Transformations.map(genreDao.observeAll(), this::merge));
    }

    @Override
    public LiveData<Map<MediaType, Map<Integer, String>>> observeGenreNames() {
        return Transformations.distinctUntilChanged(Transformations.map(genreDao.observeAll(), entities -> {
            Map<MediaType, Map<Integer, String>> names = new EnumMap<>(MediaType.class);
            for (GenreEntity entity : entities) {
                names.computeIfAbsent(entity.mediaType, type -> new HashMap<>())
                        .put(entity.genreId, GenreCatalog.displayName(entity.name));
            }
            return names;
        }));
    }

    @Nullable
    @Override
    public Genre getGenre(String key) {
        for (Genre genre : merge(genreDao.getAll())) {
            if (genre.key().equals(key)) {
                return genre;
            }
        }
        return null;
    }

    @Override
    public RefreshResult refresh(boolean force) {
        synchronized (refreshLock) {
            return RefreshResult.run(() -> refreshGenres(force));
        }
    }

    private boolean refreshGenres(boolean force) throws IOException {
        if (!force && !isStale()) {
            return false;
        }
        List<TmdbGenre> movieGenres = NetworkMappers.toTmdbGenres(TmdbCalls.execute(api.getMovieGenres()),
                MediaType.MOVIE);
        List<TmdbGenre> tvGenres = NetworkMappers.toTmdbGenres(TmdbCalls.execute(api.getTvGenres()), MediaType.TV);
        long now = timeProvider.nowMillis();
        // An empty answer must not wipe a working catalog.
        if (!movieGenres.isEmpty()) {
            genreDao.replaceForType(MediaType.MOVIE, toEntities(movieGenres, now));
        }
        if (!tvGenres.isEmpty()) {
            genreDao.replaceForType(MediaType.TV, toEntities(tvGenres, now));
        }
        return true;
    }

    private boolean isStale() {
        long now = timeProvider.nowMillis();
        for (MediaType type : MediaType.values()) {
            if (CachePolicy.isStale(genreDao.oldestCachedAt(type), CachePolicy.GENRES, now)) {
                return true;
            }
        }
        return false;
    }

    private List<Genre> merge(List<GenreEntity> entities) {
        List<TmdbGenre> genres = new ArrayList<>(entities.size());
        for (GenreEntity entity : entities) {
            genres.add(EntityMappers.toTmdbGenre(entity));
        }
        return GenreCatalog.merge(genres);
    }

    private static List<GenreEntity> toEntities(List<TmdbGenre> genres, long now) {
        List<GenreEntity> entities = new ArrayList<>(genres.size());
        for (TmdbGenre genre : genres) {
            entities.add(EntityMappers.toEntity(genre, now));
        }
        return entities;
    }
}

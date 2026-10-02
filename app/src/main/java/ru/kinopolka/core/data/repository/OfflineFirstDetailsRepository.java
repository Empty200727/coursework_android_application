package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.HttpException;
import ru.kinopolka.core.data.CachePolicy;
import ru.kinopolka.core.data.GenreCatalog;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.mapper.EntityMappers;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.database.KinopolkaDatabase;
import ru.kinopolka.core.database.dao.CastDao;
import ru.kinopolka.core.database.dao.GenreDao;
import ru.kinopolka.core.database.dao.MediaDao;
import ru.kinopolka.core.database.dao.RelatedMediaDao;
import ru.kinopolka.core.database.entity.CastMemberEntity;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbCalls;

@Singleton
public final class OfflineFirstDetailsRepository implements DetailsRepository {

    private final TmdbApi api;
    private final KinopolkaDatabase database;
    private final MediaDao mediaDao;
    private final CastDao castDao;
    private final RelatedMediaDao relatedDao;
    private final GenreDao genreDao;
    private final TimeProvider timeProvider;

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    public OfflineFirstDetailsRepository(TmdbApi api, KinopolkaDatabase database, MediaDao mediaDao, CastDao castDao,
            RelatedMediaDao relatedDao, GenreDao genreDao, TimeProvider timeProvider) {
        this.api = api;
        this.database = database;
        this.mediaDao = mediaDao;
        this.castDao = castDao;
        this.relatedDao = relatedDao;
        this.genreDao = genreDao;
        this.timeProvider = timeProvider;
    }

    @Override
    public LiveData<MediaDetails> observeDetails(MediaKey key) {
        MediaType type = key.mediaType();
        int id = key.tmdbId();
        LiveData<Map<Integer, String>> genreNames = Transformations.map(genreDao.observeAll(), genres -> {
            Map<Integer, String> names = new HashMap<>();
            for (GenreEntity genre : genres) {
                if (genre.mediaType == type) {
                    names.put(genre.genreId, GenreCatalog.displayName(genre.name));
                }
            }
            return names;
        });
        LiveData<MediaDetails> details = Transformations.switchMap(mediaDao.observe(type, id), media -> {
            if (media == null) {
                return new MutableLiveData<>(null);
            }
            return LiveDataUtils.combine(
                    List.of(
                            mediaDao.observeGenreIds(type, id),
                            genreNames,
                            castDao.observeCast(type, id),
                            relatedDao.observeRelated(type, id, RelatedKind.RECOMMENDATION),
                            relatedDao.observeRelated(type, id, RelatedKind.SIMILAR)),
                    values -> EntityMappers.toMediaDetails(
                            media,
                            LiveDataUtils.<List<Integer>>cast(values[0]),
                            LiveDataUtils.<Map<Integer, String>>cast(values[1]),
                            LiveDataUtils.<List<CastMemberEntity>>cast(values[2]),
                            LiveDataUtils.<List<MediaEntity>>cast(values[3]),
                            LiveDataUtils.<List<MediaEntity>>cast(values[4])));
        });
        return Transformations.distinctUntilChanged(details);
    }

    @Override
    public RefreshResult refreshDetails(MediaKey key, boolean force) {
        return RefreshResult.run(() -> {
            MediaEntity cached = mediaDao.get(key.mediaType(), key.tmdbId());
            Long detailsCachedAt = cached == null ? null : cached.detailsCachedAt;
            if (!force && !CachePolicy.isStale(detailsCachedAt, CachePolicy.DETAILS, timeProvider.nowMillis())) {
                return false;
            }
            store(key, withOverview(key, load(key, null, true)));
            return true;
        });
    }

    private MediaDetails load(MediaKey key, @Nullable String language, boolean withExtras) throws IOException {
        MediaDetails details = switch (key.mediaType()) {
            case MOVIE -> NetworkMappers.toMediaDetails(TmdbCalls.execute(api.getMovieDetails(
                    key.tmdbId(), withExtras ? TmdbApi.MOVIE_APPEND_TO_RESPONSE : null, language)));
            case TV -> NetworkMappers.toMediaDetails(TmdbCalls.execute(api.getTvDetails(
                    key.tmdbId(), withExtras ? TmdbApi.TV_APPEND_TO_RESPONSE : null, language)));
        };
        if (details == null) {
            throw new JsonParseException("TMDB returned no details for " + key);
        }
        return details;
    }

    /** N-07: without a Russian overview the English one is requested and marked. */
    private MediaDetails withOverview(MediaKey key, MediaDetails details) {
        if (details.media().overview() != null) {
            return details;
        }
        String english;
        try {
            english = load(key, TmdbApi.FALLBACK_LANGUAGE, false).media().overview();
        } catch (IOException | HttpException | JsonParseException ignored) {
            // The fallback is optional: the card is stored without an overview.
            english = null;
        }
        return english == null ? details : details.withEnglishOverview(english);
    }

    private void store(MediaKey key, MediaDetails details) {
        long now = timeProvider.nowMillis();
        List<Media> related = new ArrayList<>();
        Set<MediaKey> seen = new HashSet<>();
        seen.add(key);
        for (List<Media> list : List.of(details.recommendations(), details.similar())) {
            for (Media media : list) {
                if (seen.add(media.key())) {
                    related.add(media);
                }
            }
        }
        List<MediaEntity> relatedEntities = new ArrayList<>(related.size());
        for (Media media : related) {
            relatedEntities.add(EntityMappers.toEntity(media, now));
        }
        List<CastMemberEntity> cast = new ArrayList<>();
        for (CastMember member : details.cast()) {
            cast.add(EntityMappers.toEntity(member, key));
        }
        database.runInTransaction(() -> {
            mediaDao.upsertSummaries(relatedEntities);
            for (Media media : related) {
                mediaDao.replaceGenres(media.mediaType(), media.tmdbId(), media.genreIds());
            }
            mediaDao.upsert(List.of(EntityMappers.toEntity(details, now)));
            mediaDao.replaceGenres(key.mediaType(), key.tmdbId(), details.media().genreIds());
            castDao.replaceCast(key.mediaType(), key.tmdbId(), cast);
            relatedDao.replaceRelated(key.mediaType(), key.tmdbId(), RelatedKind.RECOMMENDATION,
                    EntityMappers.relatedEntities(key, RelatedKind.RECOMMENDATION,
                            without(details.recommendations(), key)));
            relatedDao.replaceRelated(key.mediaType(), key.tmdbId(), RelatedKind.SIMILAR,
                    EntityMappers.relatedEntities(key, RelatedKind.SIMILAR, without(details.similar(), key)));
        });
    }

    private static List<Media> without(List<Media> media, MediaKey key) {
        List<Media> result = new ArrayList<>();
        for (Media item : media) {
            if (!item.key().equals(key)) {
                result.add(item);
            }
        }
        return result;
    }
}

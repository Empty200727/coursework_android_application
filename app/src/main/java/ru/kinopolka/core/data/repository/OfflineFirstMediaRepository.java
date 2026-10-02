package ru.kinopolka.core.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import ru.kinopolka.core.data.CachePolicy;
import ru.kinopolka.core.data.MediaMerge;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.mapper.EntityMappers;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.data.paging.GenrePagingSource;
import ru.kinopolka.core.data.paging.SearchPagingSource;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.database.KinopolkaDatabase;
import ru.kinopolka.core.database.dao.FeedDao;
import ru.kinopolka.core.database.dao.MediaDao;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.network.DiscoverSort;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbCalls;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.PagedResponseDto;

@Singleton
public final class OfflineFirstMediaRepository implements MediaRepository {

    private static final PagingConfig PAGING_CONFIG = new PagingConfig(TmdbApi.PAGE_SIZE, TmdbApi.PAGE_SIZE, false);

    /** One cached TMDB list: the first page of an endpoint, stored in {@code feed_item} under {@code key}. */
    private record Feed(String key, @Nullable MediaType defaultType, Fetch fetch) {
    }

    @FunctionalInterface
    private interface Fetch {
        Call<PagedResponseDto<MediaListItemDto>> call(TmdbApi api);
    }

    private final TmdbApi api;
    private final KinopolkaDatabase database;
    private final MediaDao mediaDao;
    private final FeedDao feedDao;
    private final TimeProvider timeProvider;
    private final AppExecutors executors;
    private final ConcurrentMap<String, Object> feedLocks = new ConcurrentHashMap<>();

    @Inject
    public OfflineFirstMediaRepository(TmdbApi api, KinopolkaDatabase database, MediaDao mediaDao, FeedDao feedDao,
            TimeProvider timeProvider, AppExecutors executors) {
        this.api = api;
        this.database = database;
        this.mediaDao = mediaDao;
        this.feedDao = feedDao;
        this.timeProvider = timeProvider;
        this.executors = executors;
    }

    @Override
    public LiveData<List<Media>> observeShelf(Shelf shelf) {
        List<Feed> feeds = feedsOf(shelf);
        if (feeds.isEmpty()) {
            return new MutableLiveData<>(List.of());
        }
        if (feeds.size() == 1) {
            return Transformations.distinctUntilChanged(observeFeed(feeds.get(0).key()));
        }
        // «Все»: movies and series of the genre are merged by popularity.
        List<LiveData<List<Media>>> sources = new ArrayList<>();
        for (Feed feed : feeds) {
            sources.add(observeFeed(feed.key()));
        }
        return Transformations.distinctUntilChanged(LiveDataUtils.combine(sources, values -> {
            List<List<Media>> lists = new ArrayList<>();
            for (Object value : values) {
                lists.add(LiveDataUtils.cast(value));
            }
            return MediaMerge.mergeMedia(lists);
        }));
    }

    @Override
    public RefreshResult refreshShelf(Shelf shelf, boolean force) {
        List<RefreshResult> results = new ArrayList<>();
        for (Feed feed : feedsOf(shelf)) {
            results.add(refreshFeed(feed, force));
        }
        return RefreshResult.combine(results);
    }

    @Override
    public LiveData<PagingData<Media>> search(String query, MediaFilter filter) {
        return PagingLiveData.getLiveData(new Pager<>(PAGING_CONFIG,
                () -> new SearchPagingSource(api, query, filter, executors.io())));
    }

    @Override
    public LiveData<PagingData<Media>> genreMedia(Genre genre, MediaFilter filter, MediaSort sort) {
        return PagingLiveData.getLiveData(new Pager<>(PAGING_CONFIG,
                () -> new GenrePagingSource(api, genre, filter, sort, executors.io())));
    }

    private LiveData<List<Media>> observeFeed(String key) {
        return Transformations.map(feedDao.observeFeed(key), EntityMappers::toMediaList);
    }

    private RefreshResult refreshFeed(Feed feed, boolean force) {
        synchronized (feedLocks.computeIfAbsent(feed.key(), key -> new Object())) {
            return RefreshResult.run(() -> {
                Long fetchedAt = feedDao.fetchedAt(feed.key());
                if (!force && !CachePolicy.isStale(fetchedAt, CachePolicy.FEEDS, timeProvider.nowMillis())) {
                    return false;
                }
                List<Media> media = NetworkMappers.toMediaList(TmdbCalls.execute(feed.fetch().call(api)),
                        feed.defaultType());
                store(feed.key(), media.size() > TmdbApi.PAGE_SIZE ? media.subList(0, TmdbApi.PAGE_SIZE) : media);
                return true;
            });
        }
    }

    /** Titles, their genres and the feed are written in one transaction: observers never see half a feed. */
    private void store(String key, List<Media> media) {
        long now = timeProvider.nowMillis();
        List<MediaEntity> entities = new ArrayList<>(media.size());
        for (Media item : media) {
            entities.add(EntityMappers.toEntity(item, now));
        }
        database.runInTransaction(() -> {
            mediaDao.upsertSummaries(entities);
            for (Media item : media) {
                mediaDao.replaceGenres(item.mediaType(), item.tmdbId(), item.genreIds());
            }
            feedDao.replaceFeed(key, EntityMappers.feedItemEntities(key, media, now));
        });
    }

    private List<Feed> feedsOf(Shelf shelf) {
        if (shelf instanceof Shelf.Trending trending) {
            return List.of(trendingFeed(trending.filter()));
        }
        if (shelf instanceof Shelf.Popular popular) {
            return List.of(popular.mediaType() == MediaType.MOVIE
                    ? new Feed("popular:movie", MediaType.MOVIE, tmdb -> tmdb.getPopularMovies(1))
                    : new Feed("popular:tv", MediaType.TV, tmdb -> tmdb.getPopularTv(1)));
        }
        Shelf.ByGenre byGenre = (Shelf.ByGenre) shelf;
        List<Feed> feeds = new ArrayList<>();
        for (MediaType type : byGenre.filter().mediaTypes()) {
            Integer genreId = byGenre.genre().idFor(type);
            if (genreId != null) {
                feeds.add(genreFeed(genreId, type));
            }
        }
        return feeds;
    }

    private static Feed trendingFeed(MediaFilter filter) {
        return switch (filter) {
            case ALL -> new Feed("trending:all", null, tmdb -> tmdb.getTrendingWeek(TmdbApi.TRENDING_ALL, 1));
            case MOVIES -> new Feed("trending:movie", MediaType.MOVIE,
                    tmdb -> tmdb.getTrendingWeek(MediaType.MOVIE.key(), 1));
            case SERIES -> new Feed("trending:tv", MediaType.TV, tmdb -> tmdb.getTrendingWeek(MediaType.TV.key(), 1));
        };
    }

    /** Feed key as in docs/PLAN.md, section 6: {@code genre:28:movie}. */
    private static Feed genreFeed(int genreId, MediaType type) {
        DiscoverSort sort = DiscoverSort.POPULARITY;
        return new Feed("genre:" + genreId + ":" + type.key(), type, tmdb -> type == MediaType.MOVIE
                ? tmdb.discoverMovies(genreId, sort.movieValue(), TmdbApi.DEFAULT_MIN_VOTE_COUNT, 1, false)
                : tmdb.discoverTv(genreId, sort.tvValue(), TmdbApi.DEFAULT_MIN_VOTE_COUNT, 1, false));
    }
}

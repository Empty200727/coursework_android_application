package ru.kinopolka.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kinopolka.core.data.CachePolicy
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.mapper.feedItemEntities
import ru.kinopolka.core.data.mapper.toEntity
import ru.kinopolka.core.data.mapper.toMedia
import ru.kinopolka.core.data.mapper.toMediaList
import ru.kinopolka.core.data.mergeMedia
import ru.kinopolka.core.data.paging.GenrePagingSource
import ru.kinopolka.core.data.paging.SearchPagingSource
import ru.kinopolka.core.data.runRefresh
import ru.kinopolka.core.database.KinopolkaDatabase
import ru.kinopolka.core.database.dao.FeedDao
import ru.kinopolka.core.database.dao.MediaDao
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.core.network.TmdbApi
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.PagedResponseDto

@Singleton
class OfflineFirstMediaRepository @Inject constructor(
    private val api: TmdbApi,
    private val database: KinopolkaDatabase,
    private val mediaDao: MediaDao,
    private val feedDao: FeedDao,
    private val timeProvider: TimeProvider,
) : MediaRepository {

    /** One cached TMDB list: the first page of an endpoint, stored in `feed_item` under [key]. */
    private class Feed(
        val key: String,
        val defaultType: MediaType?,
        val fetch: suspend TmdbApi.() -> PagedResponseDto<MediaListItemDto>,
    )

    private val feedLocks = ConcurrentHashMap<String, Mutex>()

    override fun observeShelf(shelf: Shelf): Flow<List<Media>> {
        val feeds = feedsOf(shelf)
        return when (feeds.size) {
            0 -> flowOf(emptyList())

            1 -> observeFeed(feeds.single().key)

            // «Все»: movies and series of the genre are merged by popularity.
            else -> combine(feeds.map { observeFeed(it.key) }) { lists -> mergeMedia(lists.toList()) }
        }.distinctUntilChanged()
    }

    override suspend fun refreshShelf(shelf: Shelf, force: Boolean): RefreshResult = coroutineScope {
        val results = feedsOf(shelf).map { feed -> async { refreshFeed(feed, force) } }.awaitAll()
        results.firstOrNull { it is RefreshResult.Failed }
            ?: results.firstOrNull { it == RefreshResult.Updated }
            ?: RefreshResult.Skipped
    }

    override fun search(query: String, filter: MediaFilter): Flow<PagingData<Media>> =
        Pager(pagingConfig) { SearchPagingSource(api, query, filter) }.flow

    override fun genreMedia(genre: Genre, filter: MediaFilter, sort: MediaSort): Flow<PagingData<Media>> =
        Pager(pagingConfig) { GenrePagingSource(api, genre, filter, sort) }.flow

    private fun observeFeed(key: String): Flow<List<Media>> =
        feedDao.observeFeed(key).map { entities -> entities.map { it.toMedia() } }

    private suspend fun refreshFeed(feed: Feed, force: Boolean): RefreshResult =
        feedLocks.getOrPut(feed.key) { Mutex() }.withLock {
            runRefresh {
                val fetchedAt = feedDao.fetchedAt(feed.key)
                if (!force && !CachePolicy.isStale(fetchedAt, CachePolicy.FEEDS, timeProvider.nowMillis())) {
                    false
                } else {
                    val media = feed.fetch(api).toMediaList(feed.defaultType).take(TmdbApi.PAGE_SIZE)
                    store(feed.key, media)
                    true
                }
            }
        }

    /** Titles, their genres and the feed are written in one transaction: observers never see half a feed. */
    private suspend fun store(key: String, media: List<Media>) {
        val now = timeProvider.nowMillis()
        database.withTransaction {
            mediaDao.upsertSummaries(media.map { it.toEntity(now) })
            media.forEach { mediaDao.replaceGenres(it.mediaType, it.tmdbId, it.genreIds) }
            feedDao.replaceFeed(key, feedItemEntities(key, media, now))
        }
    }

    private fun feedsOf(shelf: Shelf): List<Feed> = when (shelf) {
        is Shelf.Trending -> listOf(trendingFeed(shelf.filter))

        is Shelf.Popular -> listOf(
            when (shelf.mediaType) {
                MediaType.MOVIE -> Feed("popular:movie", MediaType.MOVIE) { getPopularMovies() }
                MediaType.TV -> Feed("popular:tv", MediaType.TV) { getPopularTv() }
            },
        )

        is Shelf.ByGenre -> shelf.filter.mediaTypes.mapNotNull { type ->
            shelf.genre.idFor(type)?.let { genreId -> genreFeed(genreId, type) }
        }
    }

    private fun trendingFeed(filter: MediaFilter): Feed = when (filter) {
        MediaFilter.ALL -> Feed("trending:all", null) { getTrendingWeek(TmdbApi.TRENDING_ALL) }
        MediaFilter.MOVIES -> Feed("trending:movie", MediaType.MOVIE) { getTrendingWeek(MediaType.MOVIE.key) }
        MediaFilter.SERIES -> Feed("trending:tv", MediaType.TV) { getTrendingWeek(MediaType.TV.key) }
    }

    /** Feed key as in docs/PLAN.md, section 6: `genre:28:movie`. */
    private fun genreFeed(genreId: Int, type: MediaType): Feed = Feed("genre:$genreId:${type.key}", type) {
        when (type) {
            MediaType.MOVIE -> discoverMovies(genreId)
            MediaType.TV -> discoverTv(genreId)
        }
    }

    private companion object {
        val pagingConfig = PagingConfig(pageSize = TmdbApi.PAGE_SIZE, enablePlaceholders = false)
    }
}

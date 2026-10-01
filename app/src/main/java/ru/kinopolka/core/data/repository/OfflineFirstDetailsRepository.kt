package ru.kinopolka.core.data.repository

import androidx.room.withTransaction
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import ru.kinopolka.core.data.CachePolicy
import ru.kinopolka.core.data.GenreCatalog
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.mapper.relatedEntities
import ru.kinopolka.core.data.mapper.toEntity
import ru.kinopolka.core.data.mapper.toMediaDetails
import ru.kinopolka.core.data.runRefresh
import ru.kinopolka.core.database.KinopolkaDatabase
import ru.kinopolka.core.database.dao.CastDao
import ru.kinopolka.core.database.dao.GenreDao
import ru.kinopolka.core.database.dao.MediaDao
import ru.kinopolka.core.database.dao.RelatedMediaDao
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind
import ru.kinopolka.core.network.TmdbApi

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OfflineFirstDetailsRepository @Inject constructor(
    private val api: TmdbApi,
    private val database: KinopolkaDatabase,
    private val mediaDao: MediaDao,
    private val castDao: CastDao,
    private val relatedDao: RelatedMediaDao,
    private val genreDao: GenreDao,
    private val timeProvider: TimeProvider,
) : DetailsRepository {

    override fun observeDetails(key: MediaKey): Flow<MediaDetails?> {
        val type = key.mediaType
        val id = key.tmdbId
        val genreNames = genreDao.observeAll().map { genres ->
            genres.filter { it.mediaType == type }.associate { it.genreId to GenreCatalog.displayName(it.name) }
        }
        val related = combine(
            relatedDao.observeRelated(type, id, RelatedKind.RECOMMENDATION),
            relatedDao.observeRelated(type, id, RelatedKind.SIMILAR),
        ) { recommendations, similar -> recommendations to similar }

        return mediaDao.observe(type, id).flatMapLatest { media ->
            if (media == null) {
                flowOf(null)
            } else {
                combine(
                    mediaDao.observeGenreIds(type, id),
                    genreNames,
                    castDao.observeCast(type, id),
                    related,
                ) { genreIds, names, cast, (recommendations, similar) ->
                    media.toMediaDetails(genreIds, names, cast, recommendations, similar)
                }
            }
        }.distinctUntilChanged()
    }

    override suspend fun refreshDetails(key: MediaKey, force: Boolean): RefreshResult = runRefresh {
        val detailsCachedAt = mediaDao.get(key.mediaType, key.tmdbId)?.detailsCachedAt
        if (!force && !CachePolicy.isStale(detailsCachedAt, CachePolicy.DETAILS, timeProvider.nowMillis())) {
            false
        } else {
            store(key, withOverview(key, load(key, language = null)))
            true
        }
    }

    private suspend fun load(key: MediaKey, language: String?, withExtras: Boolean = true): MediaDetails {
        val details = when (key.mediaType) {
            MediaType.MOVIE -> api.getMovieDetails(
                id = key.tmdbId,
                appendToResponse = TmdbApi.MOVIE_APPEND_TO_RESPONSE.takeIf { withExtras },
                language = language,
            ).toMediaDetails()

            MediaType.TV -> api.getTvDetails(
                id = key.tmdbId,
                appendToResponse = TmdbApi.TV_APPEND_TO_RESPONSE.takeIf { withExtras },
                language = language,
            ).toMediaDetails()
        }
        return details ?: throw SerializationException("TMDB returned no details for $key")
    }

    /** N-07: without a Russian overview the English one is requested and marked. */
    private suspend fun withOverview(key: MediaKey, details: MediaDetails): MediaDetails {
        if (details.media.overview != null) return details
        val english = try {
            load(key, TmdbApi.FALLBACK_LANGUAGE, withExtras = false).media.overview
        } catch (ignored: IOException) {
            // The fallback is optional: the card is stored without an overview.
            null
        } catch (ignored: HttpException) {
            null
        }
        return if (english == null) {
            details
        } else {
            details.copy(media = details.media.copy(overview = english), isOverviewFallback = true)
        }
    }

    private suspend fun store(key: MediaKey, details: MediaDetails) {
        val now = timeProvider.nowMillis()
        val related = (details.recommendations + details.similar).distinctBy { it.key }.filter { it.key != key }
        database.withTransaction {
            mediaDao.upsertSummaries(related.map { it.toEntity(now) })
            related.forEach { mediaDao.replaceGenres(it.mediaType, it.tmdbId, it.genreIds) }
            mediaDao.upsert(listOf(details.toEntity(now)))
            mediaDao.replaceGenres(key.mediaType, key.tmdbId, details.media.genreIds)
            castDao.replaceCast(key.mediaType, key.tmdbId, details.cast.map { it.toEntity(key) })
            relatedDao.replaceRelated(
                key.mediaType,
                key.tmdbId,
                RelatedKind.RECOMMENDATION,
                relatedEntities(key, RelatedKind.RECOMMENDATION, details.recommendations.filter { it.key != key }),
            )
            relatedDao.replaceRelated(
                key.mediaType,
                key.tmdbId,
                RelatedKind.SIMILAR,
                relatedEntities(key, RelatedKind.SIMILAR, details.similar.filter { it.key != key }),
            )
        }
    }
}

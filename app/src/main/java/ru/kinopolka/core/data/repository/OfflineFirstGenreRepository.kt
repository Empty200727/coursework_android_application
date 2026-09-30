package ru.kinopolka.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kinopolka.core.data.CachePolicy
import ru.kinopolka.core.data.GenreCatalog
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.mapper.toEntity
import ru.kinopolka.core.data.mapper.toTmdbGenre
import ru.kinopolka.core.data.mapper.toTmdbGenres
import ru.kinopolka.core.data.runRefresh
import ru.kinopolka.core.database.dao.GenreDao
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.TmdbApi

@Singleton
class OfflineFirstGenreRepository @Inject constructor(
    private val api: TmdbApi,
    private val genreDao: GenreDao,
    private val timeProvider: TimeProvider,
) : GenreRepository {

    private val refreshMutex = Mutex()

    override fun observeGenres(): Flow<List<Genre>> = genreDao.observeAll()
        .map { entities -> GenreCatalog.merge(entities.map { it.toTmdbGenre() }) }
        .distinctUntilChanged()

    override fun observeGenreNames(): Flow<Map<MediaType, Map<Int, String>>> = genreDao.observeAll()
        .map { entities ->
            entities.groupBy { it.mediaType }
                .mapValues { (_, genres) -> genres.associate { it.genreId to GenreCatalog.displayName(it.name) } }
        }
        .distinctUntilChanged()

    override suspend fun getGenre(key: String): Genre? = observeGenres().first().firstOrNull { it.key == key }

    override suspend fun refresh(force: Boolean): RefreshResult = refreshMutex.withLock {
        runRefresh { refreshGenres(force) }
    }

    private suspend fun refreshGenres(force: Boolean): Boolean {
        if (!force && !isStale()) return false

        val (movieGenres, tvGenres) = coroutineScope {
            val movie = async { api.getMovieGenres().toTmdbGenres(MediaType.MOVIE) }
            val tv = async { api.getTvGenres().toTmdbGenres(MediaType.TV) }
            movie.await() to tv.await()
        }
        val now = timeProvider.nowMillis()
        // An empty answer must not wipe a working catalog.
        if (movieGenres.isNotEmpty()) {
            genreDao.replaceForType(MediaType.MOVIE, movieGenres.map { it.toEntity(now) })
        }
        if (tvGenres.isNotEmpty()) {
            genreDao.replaceForType(MediaType.TV, tvGenres.map { it.toEntity(now) })
        }
        return true
    }

    private suspend fun isStale(): Boolean {
        val now = timeProvider.nowMillis()
        return MediaType.entries.any { type ->
            CachePolicy.isStale(genreDao.oldestCachedAt(type), CachePolicy.GENRES, now)
        }
    }
}

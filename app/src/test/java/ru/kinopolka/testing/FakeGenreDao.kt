package ru.kinopolka.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.kinopolka.core.database.dao.GenreDao
import ru.kinopolka.core.database.entity.GenreEntity
import ru.kinopolka.core.model.MediaType

/** In-memory [GenreDao] for repository tests. */
class FakeGenreDao : GenreDao {
    private val rows = MutableStateFlow<Map<Pair<MediaType, Int>, GenreEntity>>(emptyMap())

    private fun sorted(values: Collection<GenreEntity>) =
        values.sortedWith(compareBy({ it.mediaType.key }, { it.genreId }))

    override fun observeAll(): Flow<List<GenreEntity>> = rows.map { sorted(it.values) }

    override suspend fun getAll(): List<GenreEntity> = sorted(rows.value.values)

    override suspend fun oldestCachedAt(mediaType: MediaType): Long? =
        rows.value.values.filter { it.mediaType == mediaType }.minOfOrNull { it.cachedAt }

    override suspend fun deleteByType(mediaType: MediaType) {
        rows.value = rows.value.filterKeys { it.first != mediaType }
    }

    override suspend fun upsert(genres: List<GenreEntity>) {
        rows.value = rows.value + genres.associateBy { it.mediaType to it.genreId }
    }

    override suspend fun replaceForType(mediaType: MediaType, genres: List<GenreEntity>) {
        rows.value = rows.value.filterKeys { it.first != mediaType } +
            genres.associateBy { it.mediaType to it.genreId }
    }
}

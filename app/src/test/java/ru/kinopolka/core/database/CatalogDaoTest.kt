package ru.kinopolka.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.database.entity.CastMemberEntity
import ru.kinopolka.core.database.entity.FeedItemEntity
import ru.kinopolka.core.database.entity.GenreEntity
import ru.kinopolka.core.database.entity.RelatedMediaEntity
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind

/** Genre, feed, cast and related-title tables. */
@RunWith(AndroidJUnit4::class)
class CatalogDaoTest : DatabaseTest() {

    @Test
    fun `genres are replaced per media type`() = runTest {
        val dao = database.genreDao()
        dao.upsert(
            listOf(
                GenreEntity(MediaType.MOVIE, 28, "боевик", 100L),
                GenreEntity(MediaType.MOVIE, 99, "устаревший", 50L),
                GenreEntity(MediaType.TV, 10759, "Боевик и Приключения", 200L),
            ),
        )
        assertEquals(50L, dao.oldestCachedAt(MediaType.MOVIE))

        dao.replaceForType(MediaType.MOVIE, listOf(GenreEntity(MediaType.MOVIE, 28, "боевик", 300L)))

        assertEquals(
            listOf(MediaType.MOVIE to 28, MediaType.TV to 10759),
            dao.observeAll().first().map { it.mediaType to it.genreId },
        )
        assertEquals(300L, dao.oldestCachedAt(MediaType.MOVIE))
        assertEquals(200L, dao.oldestCachedAt(MediaType.TV))
    }

    @Test
    fun `no genres means no cache time`() = runTest {
        assertNull(database.genreDao().oldestCachedAt(MediaType.TV))
    }

    @Test
    fun `feed keeps the order of positions`() = runTest {
        database.mediaDao().upsert(listOf(media(1), media(2), media(3, MediaType.TV)))
        val feedDao = database.feedDao()

        feedDao.replaceFeed(
            "trending",
            listOf(
                FeedItemEntity("trending", 0, MediaType.TV, 3, 500L),
                FeedItemEntity("trending", 1, MediaType.MOVIE, 1, 500L),
            ),
        )
        feedDao.replaceFeed("popular_movies", listOf(FeedItemEntity("popular_movies", 0, MediaType.MOVIE, 2, 700L)))

        assertEquals(listOf(3, 1), feedDao.observeFeed("trending").first().map { it.tmdbId })
        assertEquals(500L, feedDao.fetchedAt("trending"))
        assertNull(feedDao.fetchedAt("unknown"))

        feedDao.replaceFeed("trending", listOf(FeedItemEntity("trending", 0, MediaType.MOVIE, 2, 900L)))
        assertEquals(listOf(2), feedDao.observeFeed("trending").first().map { it.tmdbId })
        assertEquals(listOf(2), feedDao.observeFeed("popular_movies").first().map { it.tmdbId })
    }

    @Test
    fun `cast is ordered by billing`() = runTest {
        database.mediaDao().upsert(listOf(media(550)))
        val castDao = database.castDao()

        castDao.replaceCast(
            MediaType.MOVIE,
            550,
            listOf(
                CastMemberEntity(MediaType.MOVIE, 550, 287, "Брэд Питт", "Тайлер Дёрден", null, 1),
                CastMemberEntity(MediaType.MOVIE, 550, 819, "Эдвард Нортон", "Рассказчик", null, 0),
            ),
        )

        assertEquals(listOf(819, 287), castDao.observeCast(MediaType.MOVIE, 550).first().map { it.personId })
    }

    @Test
    fun `recommendations and similar titles are stored separately`() = runTest {
        database.mediaDao().upsert(listOf(media(550), media(807), media(680), media(1954)))
        val dao = database.relatedMediaDao()

        dao.replaceRelated(
            MediaType.MOVIE,
            550,
            RelatedKind.RECOMMENDATION,
            listOf(
                RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION, MediaType.MOVIE, 680, 1),
                RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION, MediaType.MOVIE, 807, 0),
            ),
        )
        dao.replaceRelated(
            MediaType.MOVIE,
            550,
            RelatedKind.SIMILAR,
            listOf(RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.SIMILAR, MediaType.MOVIE, 1954, 0)),
        )

        assertEquals(
            listOf(807, 680),
            dao.observeRelated(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION).first().map { it.tmdbId },
        )
        assertEquals(
            listOf(1954),
            dao.observeRelated(MediaType.MOVIE, 550, RelatedKind.SIMILAR).first().map {
                it.tmdbId
            },
        )
    }
}

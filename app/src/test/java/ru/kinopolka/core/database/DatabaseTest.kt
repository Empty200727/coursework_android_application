package ru.kinopolka.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import ru.kinopolka.core.database.entity.MediaEntity
import ru.kinopolka.core.model.MediaType

/** Base class for DAO tests: a fresh in-memory database per test. */
abstract class DatabaseTest {

    protected lateinit var database: KinopolkaDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KinopolkaDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    protected fun media(
        id: Int,
        type: MediaType = MediaType.MOVIE,
        title: String = "Фильм $id",
        cachedAt: Long = 1_000L,
        runtime: Int? = null,
        detailsCachedAt: Long? = null,
    ) = MediaEntity(
        mediaType = type,
        tmdbId = id,
        title = title,
        originalTitle = null,
        overview = "Описание $id",
        isOverviewFallback = false,
        posterPath = "/$id.jpg",
        backdropPath = null,
        releaseDate = "2020-01-01",
        lastAirDate = null,
        voteAverage = 7.5,
        voteCount = 1000,
        popularity = 10.0,
        runtime = runtime,
        numberOfSeasons = null,
        inProduction = null,
        cachedAt = cachedAt,
        detailsCachedAt = detailsCachedAt,
    )
}

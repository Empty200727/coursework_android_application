package ru.kinopolka.core.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.testing.testMedia

/** «Все»: movies and series of a shelf are merged into one list (docs/PLAN.md, section 4). */
class MediaMergeTest {

    private val movies = listOf(
        testMedia(1, MediaType.MOVIE, popularity = 90.0),
        testMedia(2, MediaType.MOVIE, popularity = 50.0),
        testMedia(3, MediaType.MOVIE, popularity = 10.0),
    )
    private val series = listOf(
        testMedia(1, MediaType.TV, popularity = 70.0),
        testMedia(4, MediaType.TV, popularity = 20.0),
    )

    @Test
    fun `movies and series are merged by popularity`() {
        val merged = mergeMedia(listOf(movies, series))

        assertEquals(
            listOf(
                MediaKey(MediaType.MOVIE, 1),
                MediaKey(MediaType.TV, 1),
                MediaKey(MediaType.MOVIE, 2),
                MediaKey(MediaType.TV, 4),
                MediaKey(MediaType.MOVIE, 3),
            ),
            merged.map { it.key },
        )
    }

    @Test
    fun `merged shelf keeps 20 items`() {
        val manyMovies = (1..20).map { testMedia(it, MediaType.MOVIE, popularity = it.toDouble()) }
        val manySeries = (1..20).map { testMedia(it, MediaType.TV, popularity = it + 0.5) }

        val merged = mergeMedia(listOf(manyMovies, manySeries))

        assertEquals(20, merged.size)
        assertEquals(MediaKey(MediaType.TV, 20), merged.first().key)
        assertEquals(11.0, merged.last().popularity, 0.0)
    }

    @Test
    fun `duplicates and empty lists are handled`() {
        assertEquals(movies.map { it.key }, mergeMedia(listOf(movies, emptyList(), movies)).map { it.key })
        assertEquals(emptyList<Any>(), mergeMedia(emptyList()))
    }

    @Test
    fun `other sort orders are supported`() {
        val rated = listOf(
            testMedia(1, voteAverage = 6.0, releaseDate = LocalDate.of(2024, 1, 1)),
            testMedia(2, voteAverage = 9.0, releaseDate = null),
            testMedia(3, MediaType.TV, voteAverage = 8.0, releaseDate = LocalDate.of(2025, 5, 1)),
        )

        assertEquals(listOf(2, 3, 1), mergeMedia(listOf(rated), MediaSort.RATING).map { it.tmdbId })
        assertEquals(listOf(3, 1, 2), mergeMedia(listOf(rated), MediaSort.NEWEST).map { it.tmdbId })
    }
}

package ru.kinopolka.core.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.testing.FakeTmdbApi
import ru.kinopolka.testing.movieDto
import ru.kinopolka.testing.pageOf
import ru.kinopolka.testing.tvDto

class PagingSourcesTest {

    private val api = FakeTmdbApi()
    private val config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    private val action = Genre(key = "action", name = "Боевик", movieGenreId = 28, tvGenreId = 10759)
    private val horror = Genre(key = "horror", name = "Ужасы", movieGenreId = 27, tvGenreId = null)

    private fun PagingSource.LoadResult<Int, Media>.page() = this as PagingSource.LoadResult.Page<Int, Media>

    @Test
    fun `search without filter drops people`() = runTest {
        val page = TestPager(config, SearchPagingSource(api, "тяжкие", MediaFilter.ALL)).refresh().page()

        assertEquals(listOf(MediaKey(MediaType.TV, 1396), MediaKey(MediaType.MOVIE, 559969)), page.data.map { it.key })
        assertNull("single page", page.nextKey)
        assertEquals(listOf("search/multi:тяжкие:1"), api.requests)
    }

    @Test
    fun `search with a filter uses the endpoint of the type`() = runTest {
        api.search = { _, _, page -> pageOf(listOf(movieDto(page * 10)), page = page, totalPages = 3) }

        val pager = TestPager(config, SearchPagingSource(api, "матрица", MediaFilter.MOVIES))
        val first = pager.refresh().page()
        val second = pager.append()?.page()

        assertEquals(listOf(MediaKey(MediaType.MOVIE, 10)), first.data.map { it.key })
        assertEquals(2, first.nextKey)
        assertEquals(3, second?.nextKey)
        assertEquals(listOf("search/movie:матрица:1", "search/movie:матрица:2"), api.requests)
    }

    @Test
    fun `network errors become an error result`() = runTest {
        api.failure = IOException("offline")

        val result = TestPager(config, SearchPagingSource(api, "дюна", MediaFilter.SERIES)).refresh()

        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `genre page merges movies and series by the selected sort`() = runTest {
        api.discoverMoviesPage = { _, _, page ->
            pageOf(listOf(movieDto(1, voteAverage = 9.0), movieDto(2, voteAverage = 6.0)), page, totalPages = 2)
        }
        api.discoverTvPage = { _, _, page -> pageOf(listOf(tvDto(1, voteAverage = 7.5)), page, totalPages = 1) }

        val page = TestPager(config, GenrePagingSource(api, action, MediaFilter.ALL, MediaSort.RATING)).refresh().page()

        assertEquals(
            listOf(MediaKey(MediaType.MOVIE, 1), MediaKey(MediaType.TV, 1), MediaKey(MediaType.MOVIE, 2)),
            page.data.map { it.key },
        )
        assertEquals("movies have one more page", 2, page.nextKey)
        assertTrue("discover/movie:28:vote_average.desc:1" in api.requests)
        assertTrue("discover/tv:10759:vote_average.desc:1" in api.requests)
    }

    @Test
    fun `genre without series requests only movies`() = runTest {
        TestPager(config, GenrePagingSource(api, horror, MediaFilter.ALL, MediaSort.NEWEST)).refresh()

        assertEquals(listOf("discover/movie:27:primary_release_date.desc:1"), api.requests)
    }

    @Test
    fun `titles repeated on the next page are dropped`() = runTest {
        api.discoverMoviesPage = { _, _, page ->
            val ids = if (page == 1) listOf(1, 2) else listOf(2, 3)
            pageOf(ids.map { movieDto(it) }, page, totalPages = 2)
        }
        val pager = TestPager(config, GenrePagingSource(api, action, MediaFilter.MOVIES, MediaSort.POPULARITY))

        pager.refresh()
        val second = pager.append()?.page()

        assertEquals(listOf(3), second?.data?.map { it.tmdbId })
        assertNull(second?.nextKey)
    }
}

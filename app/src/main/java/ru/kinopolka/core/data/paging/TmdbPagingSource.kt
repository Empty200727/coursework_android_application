package ru.kinopolka.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.network.TmdbApi

/**
 * Pages of TMDB results (20 per page, F-03). Titles already shown on earlier pages are
 * dropped: TMDB rankings shift between requests and a repeated key would break the list.
 */
abstract class TmdbPagingSource : PagingSource<Int, Media>() {

    private val seenKeys = mutableSetOf<MediaKey>()

    /** Loads one TMDB page; returns its titles and whether more pages exist. */
    protected abstract suspend fun loadPage(page: Int): PageResult

    protected data class PageResult(val items: List<Media>, val hasMore: Boolean)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Media> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val result = loadPage(page)
            val items = synchronized(seenKeys) { result.items.filter { seenKeys.add(it.key) } }
            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = if (result.hasMore && page < TmdbApi.MAX_PAGE) page + 1 else null,
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: SerializationException) {
            LoadResult.Error(e)
        }
    }

    // Pages are loaded forward only: a refresh starts from the first page.
    override fun getRefreshKey(state: PagingState<Int, Media>): Int? = null

    protected fun hasMore(page: Int, totalPages: Int?): Boolean = page < (totalPages ?: page)

    companion object {
        const val FIRST_PAGE = 1
    }
}

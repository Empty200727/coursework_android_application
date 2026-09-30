package ru.kinopolka.core.data.paging

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.kinopolka.core.data.mapMediaSort
import ru.kinopolka.core.data.mapper.toMediaList
import ru.kinopolka.core.data.mergeMedia
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.TmdbApi

/**
 * Full list of a genre (F-06) from discover with `vote_count.gte=100`. With «Все» each page
 * joins the same page of movies and series and orders it by [sort].
 */
class GenrePagingSource(
    private val api: TmdbApi,
    private val genre: Genre,
    private val filter: MediaFilter,
    private val sort: MediaSort,
) : TmdbPagingSource() {

    override suspend fun loadPage(page: Int): PageResult = coroutineScope {
        val types = filter.mediaTypes.filter { genre.idFor(it) != null }
        val pages = types.map { type ->
            async {
                val genreId = requireNotNull(genre.idFor(type))
                val discoverSort = mapMediaSort(sort)
                val response = when (type) {
                    MediaType.MOVIE -> api.discoverMovies(genreId, sortBy = discoverSort.movieValue, page = page)
                    MediaType.TV -> api.discoverTv(genreId, sortBy = discoverSort.tvValue, page = page)
                }
                response.toMediaList(type) to hasMore(page, response.totalPages)
            }
        }.awaitAll()
        PageResult(
            items = mergeMedia(pages.map { it.first }, sort, limit = Int.MAX_VALUE),
            hasMore = pages.any { it.second },
        )
    }
}

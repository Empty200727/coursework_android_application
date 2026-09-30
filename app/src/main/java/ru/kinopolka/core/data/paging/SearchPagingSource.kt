package ru.kinopolka.core.data.paging

import ru.kinopolka.core.data.mapper.toMediaList
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.TmdbApi

/**
 * Search (F-01): search/multi for «Все» with people dropped by the mapper,
 * search/movie or search/tv for a single type.
 */
class SearchPagingSource(private val api: TmdbApi, private val query: String, private val filter: MediaFilter) :
    TmdbPagingSource() {

    override suspend fun loadPage(page: Int): PageResult {
        val response = when (filter) {
            MediaFilter.ALL -> api.searchMulti(query, page)
            MediaFilter.MOVIES -> api.searchMovies(query, page)
            MediaFilter.SERIES -> api.searchTv(query, page)
        }
        val defaultType = when (filter) {
            MediaFilter.ALL -> null
            MediaFilter.MOVIES -> MediaType.MOVIE
            MediaFilter.SERIES -> MediaType.TV
        }
        return PageResult(
            items = response.toMediaList(defaultType).filter { filter.includes(it.mediaType) },
            hasMore = hasMore(page, response.totalPages),
        )
    }
}

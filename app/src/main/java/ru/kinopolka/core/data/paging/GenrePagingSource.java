package ru.kinopolka.core.data.paging;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ru.kinopolka.core.data.MediaMerge;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.network.DiscoverSort;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbCalls;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.PagedResponseDto;

/**
 * Full list of a genre (F-06) from discover with {@code vote_count.gte=100}. With «Все» each page
 * joins the same page of movies and series and orders it by the sort.
 */
public final class GenrePagingSource extends TmdbPagingSource {

    private final TmdbApi api;
    private final Genre genre;
    private final MediaFilter filter;
    private final MediaSort sort;

    public GenrePagingSource(TmdbApi api, Genre genre, MediaFilter filter, MediaSort sort,
            ListeningExecutorService executor) {
        super(executor);
        this.api = api;
        this.genre = genre;
        this.filter = filter;
        this.sort = sort;
    }

    @Override
    protected PageResult loadPage(int page) throws IOException {
        DiscoverSort discoverSort = MediaMerge.mapMediaSort(sort);
        List<List<Media>> pages = new ArrayList<>();
        boolean hasMore = false;
        for (MediaType type : filter.mediaTypes()) {
            Integer genreId = genre.idFor(type);
            if (genreId == null) {
                continue;
            }
            int votes = TmdbApi.DEFAULT_MIN_VOTE_COUNT;
            PagedResponseDto<MediaListItemDto> response = TmdbCalls.execute(type == MediaType.MOVIE
                    ? api.discoverMovies(genreId, discoverSort.movieValue(), votes, page, false)
                    : api.discoverTv(genreId, discoverSort.tvValue(), votes, page, false));
            pages.add(NetworkMappers.toMediaList(response, type));
            hasMore |= hasMore(page, response.totalPages());
        }
        return new PageResult(MediaMerge.mergeMedia(pages, sort, Integer.MAX_VALUE), hasMore);
    }
}

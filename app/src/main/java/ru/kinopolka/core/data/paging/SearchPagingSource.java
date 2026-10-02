package ru.kinopolka.core.data.paging;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ru.kinopolka.core.data.mapper.NetworkMappers;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbCalls;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.PagedResponseDto;

/**
 * Search (F-01): search/multi for «Все» with people dropped by the mapper,
 * search/movie or search/tv for a single type.
 */
public final class SearchPagingSource extends TmdbPagingSource {

    private final TmdbApi api;
    private final String query;
    private final MediaFilter filter;

    public SearchPagingSource(TmdbApi api, String query, MediaFilter filter, ListeningExecutorService executor) {
        super(executor);
        this.api = api;
        this.query = query;
        this.filter = filter;
    }

    @Override
    protected PageResult loadPage(int page) throws IOException {
        PagedResponseDto<MediaListItemDto> response = TmdbCalls.execute(switch (filter) {
            case ALL -> api.searchMulti(query, page, false);
            case MOVIES -> api.searchMovies(query, page, false);
            case SERIES -> api.searchTv(query, page, false);
        });
        MediaType defaultType = switch (filter) {
            case ALL -> null;
            case MOVIES -> MediaType.MOVIE;
            case SERIES -> MediaType.TV;
        };
        List<Media> items = new ArrayList<>();
        for (Media media : NetworkMappers.toMediaList(response, defaultType)) {
            if (filter.includes(media.mediaType())) {
                items.add(media);
            }
        }
        return new PageResult(items, hasMore(page, response.totalPages()));
    }
}

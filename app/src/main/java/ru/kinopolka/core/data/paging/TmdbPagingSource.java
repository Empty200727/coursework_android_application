package ru.kinopolka.core.data.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.ListenableFuturePagingSource;
import androidx.paging.PagingState;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.network.TmdbApi;

/**
 * Pages of TMDB results (20 per page, F-03). Titles already shown on earlier pages are
 * dropped: TMDB rankings shift between requests and a repeated key would break the list.
 */
public abstract class TmdbPagingSource extends ListenableFuturePagingSource<Integer, Media> {

    public static final int FIRST_PAGE = 1;

    private final ListeningExecutorService executor;
    private final Set<MediaKey> seenKeys = new HashSet<>();

    protected TmdbPagingSource(ListeningExecutorService executor) {
        this.executor = executor;
    }

    /** One TMDB page: its titles and whether more pages exist. */
    protected record PageResult(List<Media> items, boolean hasMore) {
    }

    /** Loads one TMDB page on a background thread. */
    protected abstract PageResult loadPage(int page) throws IOException;

    @NonNull
    @Override
    public ListenableFuture<LoadResult<Integer, Media>> loadFuture(@NonNull LoadParams<Integer> params) {
        Integer key = params.getKey();
        int page = key != null ? key : FIRST_PAGE;
        return executor.submit(() -> load(page));
    }

    /** Synchronous load, also used by tests. */
    public LoadResult<Integer, Media> load(int page) {
        try {
            PageResult result = loadPage(page);
            List<Media> items = new ArrayList<>();
            synchronized (seenKeys) {
                for (Media media : result.items()) {
                    if (seenKeys.add(media.key())) {
                        items.add(media);
                    }
                }
            }
            Integer nextKey = result.hasMore() && page < TmdbApi.MAX_PAGE ? page + 1 : null;
            return new LoadResult.Page<>(items, null, nextKey);
        } catch (IOException | RuntimeException e) {
            if (!DataError.isExpected(e)) {
                throw e instanceof RuntimeException runtime ? runtime : new IllegalStateException(e);
            }
            return new LoadResult.Error<>(e);
        }
    }

    // Pages are loaded forward only: a refresh starts from the first page.
    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Media> state) {
        return null;
    }

    protected static boolean hasMore(int page, @Nullable Integer totalPages) {
        return page < (totalPages != null ? totalPages : page);
    }
}

package ru.kinopolka.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LoadState;
import androidx.paging.LoadStates;
import androidx.paging.PagingData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.Shelf;

public final class FakeMediaRepository implements MediaRepository {

    /** A started search: query and filter. */
    public record Search(String query, MediaFilter filter) {
    }

    private final Map<Shelf, MutableLiveData<List<Media>>> shelves = new HashMap<>();
    public final List<Shelf> refreshedShelves = Collections.synchronizedList(new ArrayList<>());
    public volatile RefreshResult refreshResult = RefreshResult.UPDATED;

    /** Called on refresh, e.g. to put data into the shelves. */
    public volatile Consumer<Shelf> onRefresh = shelf -> { };

    public final List<Search> searches = new ArrayList<>();
    public BiFunction<String, MediaFilter, LiveData<PagingData<Media>>> searchResults =
            (query, filter) -> pages(List.of());
    public LiveData<PagingData<Media>> genreResults = pages(List.of());

    /** Static results that report a finished load, like a real pager after its last page. */
    public static LiveData<PagingData<Media>> pages(List<Media> items) {
        LoadState done = new LoadState.NotLoading(true);
        return new MutableLiveData<>(PagingData.from(items, new LoadStates(done, done, done)));
    }

    private synchronized MutableLiveData<List<Media>> shelf(Shelf shelf) {
        return shelves.computeIfAbsent(shelf, key -> new MutableLiveData<>(List.of()));
    }

    /** Puts titles on a shelf; may be called from any thread. */
    public void setShelf(Shelf shelf, List<Media> items) {
        shelf(shelf).postValue(items);
    }

    @Override
    public LiveData<List<Media>> observeShelf(Shelf shelf) {
        return shelf(shelf);
    }

    @Override
    public RefreshResult refreshShelf(Shelf shelf, boolean force) {
        refreshedShelves.add(shelf);
        onRefresh.accept(shelf);
        return refreshResult;
    }

    @Override
    public LiveData<PagingData<Media>> search(String query, MediaFilter filter) {
        searches.add(new Search(query, filter));
        return searchResults.apply(query, filter);
    }

    @Override
    public LiveData<PagingData<Media>> genreMedia(Genre genre, MediaFilter filter, MediaSort sort) {
        return genreResults;
    }
}

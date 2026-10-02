package ru.kinopolka.feature.search;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Map;
import javax.inject.Inject;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.data.util.NetworkMonitor;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.ui.SavedStateValues;

/**
 * Search (F-01…F-03). The query and the filter live in {@link SavedStateHandle} (N-05). A request
 * goes out 400 ms after typing stops, from 2 characters; a new query replaces the previous one.
 */
@HiltViewModel
public class SearchViewModel extends ViewModel {

    static final String QUERY_KEY = "query";
    static final String FILTER_KEY = "filter";

    private record Request(String query, MediaFilter filter) {
    }

    private final SavedStateHandle savedStateHandle;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<String> debouncedQuery = new MutableLiveData<>();
    private final MutableLiveData<String> resultsQuery = new MutableLiveData<>(null);
    private final LiveData<PagingData<Media>> results;
    private final MediatorLiveData<SearchUiState> uiState = new MediatorLiveData<>();

    private String query;
    private MediaFilter filter;
    private Map<MediaType, Map<Integer, String>> genreNames = Map.of();
    private boolean online = true;

    @Inject
    public SearchViewModel(SavedStateHandle savedStateHandle, MediaRepository mediaRepository,
            GenreRepository genreRepository, NetworkMonitor networkMonitor, AppExecutors executors) {
        this.savedStateHandle = savedStateHandle;
        String savedQuery = savedStateHandle.get(QUERY_KEY);
        query = savedQuery != null ? savedQuery : "";
        filter = SavedStateValues.get(savedStateHandle, FILTER_KEY, MediaFilter.ALL);
        uiState.setValue(new SearchUiState(query, filter, genreNames, false, null));

        LiveData<String> queryData = savedStateHandle.getLiveData(QUERY_KEY, "");
        LiveData<MediaFilter> filterData = SavedStateValues.enumLiveData(savedStateHandle, FILTER_KEY, MediaFilter.ALL);

        // Only the query is debounced: switching the filter searches again at once.
        MediatorLiveData<String> debouncer = new MediatorLiveData<>();
        debouncer.addSource(queryData, value -> {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(() -> debouncedQuery.setValue(value.trim()), SearchUiState.DEBOUNCE_MILLIS);
        });
        LiveData<Request> requests = Transformations.distinctUntilChanged(LiveDataUtils.combine(
                Transformations.distinctUntilChanged(debouncedQuery), filterData, Request::new));
        // switchMap drops the previous result list: its pages are no longer loaded. The cache keeps
        // the current pages across rotation.
        LiveData<PagingData<Media>> paged = PagingLiveData.cachedIn(Transformations.switchMap(requests, request -> {
            resultsQuery.setValue(request.query());
            if (request.query().length() < SearchUiState.MIN_QUERY_LENGTH) {
                return new MutableLiveData<>(PagingData.empty());
            }
            return mediaRepository.search(request.query(), request.filter());
        }), ViewModelKt.getViewModelScope(this));
        MediatorLiveData<PagingData<Media>> resultsWithDebounce = new MediatorLiveData<>();
        resultsWithDebounce.addSource(debouncer, ignored -> { });
        resultsWithDebounce.addSource(paged, resultsWithDebounce::setValue);
        results = resultsWithDebounce;

        uiState.addSource(queryData, value -> {
            query = value;
            publish();
        });
        uiState.addSource(filterData, value -> {
            filter = value;
            publish();
        });
        uiState.addSource(genreRepository.observeGenreNames(), value -> {
            genreNames = value;
            publish();
        });
        uiState.addSource(networkMonitor.isOnline(), value -> {
            online = value;
            publish();
        });
        uiState.addSource(resultsQuery, value -> publish());

        // Result rows show genre names from the catalog: make sure it is loaded.
        executors.io().execute(() -> genreRepository.refresh(false));
    }

    public LiveData<SearchUiState> getUiState() {
        return uiState;
    }

    /** Results of the current query; observing them also runs the debounce. */
    public LiveData<PagingData<Media>> getResults() {
        return results;
    }

    public void onQueryChange(String newQuery) {
        if (!newQuery.equals(savedStateHandle.get(QUERY_KEY))) {
            savedStateHandle.set(QUERY_KEY, newQuery);
        }
    }

    public void onFilterChange(MediaFilter newFilter) {
        savedStateHandle.set(FILTER_KEY, newFilter.name());
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
    }

    private void publish() {
        uiState.setValue(new SearchUiState(query, filter, genreNames, !online, resultsQuery.getValue()));
    }
}

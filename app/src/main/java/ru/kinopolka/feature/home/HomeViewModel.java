package ru.kinopolka.feature.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.data.util.NetworkMonitor;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.ui.RefreshState;
import ru.kinopolka.core.ui.SavedStateValues;
import ru.kinopolka.feature.home.HomeUiState.ShelfContent;

/**
 * Home screen (F-02, F-05, F-07): shelves are read from Room, stale ones are refreshed in the
 * background, and everything is refreshed again when the connection comes back (N-02).
 */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    static final String FILTER_KEY = "filter";

    private final SavedStateHandle savedStateHandle;
    private final MediaRepository mediaRepository;
    private final GenreRepository genreRepository;
    private final AppExecutors executors;

    private final LiveData<Boolean> online;
    private final LiveData<List<Shelf>> shelves;
    private final MutableLiveData<RefreshState> refreshState = new MutableLiveData<>(RefreshState.LOADING);
    private final MutableLiveData<Integer> retryRequests = new MutableLiveData<>(0);
    private final LiveData<RefreshRequest> refreshRequests;
    private final Observer<RefreshRequest> refreshObserver = this::refresh;
    private final AtomicInteger refreshGeneration = new AtomicInteger();
    private final MediatorLiveData<HomeUiState> uiState = new MediatorLiveData<>(HomeUiState.INITIAL);

    private MediaFilter filter;
    private List<ShelfContent> contents = List.of();
    private boolean isOnline = true;

    @Inject
    public HomeViewModel(SavedStateHandle savedStateHandle, MediaRepository mediaRepository,
            GenreRepository genreRepository, NetworkMonitor networkMonitor, AppExecutors executors) {
        this.savedStateHandle = savedStateHandle;
        this.mediaRepository = mediaRepository;
        this.genreRepository = genreRepository;
        this.executors = executors;
        this.online = networkMonitor.isOnline();
        this.filter = SavedStateValues.get(savedStateHandle, FILTER_KEY, MediaFilter.ALL);

        LiveData<MediaFilter> filterData = SavedStateValues.enumLiveData(savedStateHandle, FILTER_KEY, MediaFilter.ALL);
        shelves = Transformations.distinctUntilChanged(
                LiveDataUtils.combine(filterData, genreRepository.observeGenres(), HomeShelves::homeShelves));
        LiveData<List<ShelfContent>> shelfContents = Transformations.switchMap(shelves, this::observeContents);

        uiState.addSource(filterData, value -> {
            filter = value;
            publish();
        });
        uiState.addSource(shelfContents, value -> {
            contents = value;
            publish();
        });
        uiState.addSource(refreshState, value -> publish());
        uiState.addSource(online, value -> {
            isOnline = value;
            publish();
        });

        // Refreshes start at once and do not wait for the screen: shelves, the connection and
        // «Повторить» restart them.
        refreshRequests = LiveDataUtils.combine(List.of(shelves, online, retryRequests),
                values -> new RefreshRequest(LiveDataUtils.cast(values[0]), (Boolean) values[1], (Integer) values[2]));
        refreshRequests.observeForever(refreshObserver);
    }

    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void onFilterChange(MediaFilter newFilter) {
        savedStateHandle.set(FILTER_KEY, newFilter.name());
    }

    public void retry() {
        Integer current = retryRequests.getValue();
        retryRequests.setValue(current == null ? 1 : current + 1);
    }

    @Override
    protected void onCleared() {
        refreshRequests.removeObserver(refreshObserver);
    }

    private LiveData<List<ShelfContent>> observeContents(List<Shelf> list) {
        List<LiveData<ShelfContent>> sources = new ArrayList<>();
        for (Shelf shelf : list) {
            sources.add(Transformations.map(mediaRepository.observeShelf(shelf),
                    items -> new ShelfContent(shelf, items)));
        }
        return LiveDataUtils.combine(sources, values -> {
            List<ShelfContent> result = new ArrayList<>(values.length);
            for (Object value : values) {
                result.add((ShelfContent) value);
            }
            return result;
        });
    }

    private void publish() {
        List<ShelfContent> visible = new ArrayList<>();
        for (ShelfContent content : contents) {
            if (HomeShelves.matches(content.shelf(), filter)) {
                visible.add(content);
            }
        }
        RefreshState refresh = refreshState.getValue();
        uiState.setValue(new HomeUiState(filter, visible, refresh.refreshing(), refresh.error(), !isOnline));
    }

    /** What a refresh depends on; a change of any part starts a new refresh. */
    private record RefreshRequest(List<Shelf> shelves, Boolean online, Integer retry) {
    }

    /** A newer refresh replaces an older one: results of the old one are ignored. */
    private void refresh(RefreshRequest request) {
        List<Shelf> current = request.shelves();
        int generation = refreshGeneration.incrementAndGet();
        if (!Boolean.TRUE.equals(request.online())) {
            refreshState.setValue(RefreshState.OFFLINE);
            return;
        }
        refreshState.setValue(RefreshState.LOADING);
        ListenableFuture<List<RefreshResult>> results = Futures.transformAsync(
                executors.io().submit(() -> genreRepository.refresh(false)),
                genres -> {
                    List<ListenableFuture<RefreshResult>> all = new ArrayList<>();
                    all.add(Futures.immediateFuture(genres));
                    for (Shelf shelf : current) {
                        all.add(executors.io().submit(() -> mediaRepository.refreshShelf(shelf, false)));
                    }
                    return Futures.allAsList(all);
                },
                MoreExecutors.directExecutor());
        results.addListener(() -> {
            if (generation != refreshGeneration.get()) {
                return;
            }
            DataError error = null;
            try {
                for (RefreshResult result : Futures.getDone(results)) {
                    if (result instanceof RefreshResult.Failed failed) {
                        error = failed.error();
                        break;
                    }
                }
            } catch (ExecutionException e) {
                throw new IllegalStateException(e.getCause());
            }
            refreshState.postValue(new RefreshState(false, error));
        }, MoreExecutors.directExecutor());
    }
}

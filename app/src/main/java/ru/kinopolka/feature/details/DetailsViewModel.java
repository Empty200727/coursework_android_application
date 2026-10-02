package ru.kinopolka.feature.details;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import ru.kinopolka.core.data.repository.DetailsRepository;
import ru.kinopolka.core.data.repository.LibraryRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.data.util.NetworkMonitor;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.core.ui.RefreshState;
import ru.kinopolka.navigation.Navigator;

/**
 * Title card (F-08…F-11): the cached card is shown at once, stale data (24 h) is refreshed;
 * «Хочу посмотреть», «Смотрел» and «В избранное» change the library.
 */
@HiltViewModel
public class DetailsViewModel extends ViewModel {

    private final MediaKey key;
    private final DetailsRepository detailsRepository;
    private final LibraryRepository libraryRepository;
    private final AppExecutors executors;

    private final LiveData<Boolean> online;
    private final MutableLiveData<RefreshState> refreshState = new MutableLiveData<>(RefreshState.LOADING);
    private final MutableLiveData<Integer> retryRequests = new MutableLiveData<>(0);
    private final LiveData<Boolean> refreshRequests;
    private final Observer<Boolean> refreshObserver = this::refresh;
    private final AtomicInteger refreshGeneration = new AtomicInteger();
    private final MediatorLiveData<DetailsUiState> uiState = new MediatorLiveData<>();

    private MediaDetails details;
    private LibraryEntry entry;
    private boolean isOnline = true;

    @Inject
    public DetailsViewModel(SavedStateHandle savedStateHandle, DetailsRepository detailsRepository,
            LibraryRepository libraryRepository, NetworkMonitor networkMonitor, AppExecutors executors) {
        String type = Objects.requireNonNull(savedStateHandle.get(Navigator.ARG_MEDIA_TYPE));
        Integer id = Objects.requireNonNull(savedStateHandle.get(Navigator.ARG_ID));
        this.key = Navigator.mediaKey(type, id);
        this.detailsRepository = detailsRepository;
        this.libraryRepository = libraryRepository;
        this.executors = executors;
        this.online = networkMonitor.isOnline();
        uiState.setValue(new DetailsUiState(key, null, null, true, null, false));

        uiState.addSource(detailsRepository.observeDetails(key), value -> {
            details = value;
            publish();
        });
        uiState.addSource(libraryRepository.observeEntry(key), value -> {
            entry = value;
            publish();
        });
        uiState.addSource(refreshState, value -> publish());
        uiState.addSource(online, value -> {
            isOnline = value;
            publish();
        });

        // The connection and «Повторить» start a refresh; the value is the connection state.
        refreshRequests = LiveDataUtils.combine(online, retryRequests, (connected, retry) -> connected);
        refreshRequests.observeForever(refreshObserver);
    }

    public LiveData<DetailsUiState> getUiState() {
        return uiState;
    }

    /** The selected status is cleared by a second tap; «Хочу» and «Смотрел» exclude each other. */
    public void onStatusClick(WatchStatus status) {
        WatchStatus current = uiState.getValue().watchStatus();
        WatchStatus next = current == status ? WatchStatus.NONE : status;
        executors.io().execute(() -> libraryRepository.setWatchStatus(key, next));
    }

    public void onFavoriteClick() {
        boolean favorite = !uiState.getValue().favorite();
        executors.io().execute(() -> libraryRepository.setFavorite(key, favorite));
    }

    public void retry() {
        Integer current = retryRequests.getValue();
        retryRequests.setValue(current == null ? 1 : current + 1);
    }

    @Override
    protected void onCleared() {
        refreshRequests.removeObserver(refreshObserver);
    }

    private void refresh(Boolean connected) {
        int generation = refreshGeneration.incrementAndGet();
        if (!Boolean.TRUE.equals(connected)) {
            refreshState.setValue(RefreshState.OFFLINE);
            return;
        }
        refreshState.setValue(RefreshState.LOADING);
        executors.io().execute(() -> {
            RefreshState result = RefreshState.finished(detailsRepository.refreshDetails(key, false));
            if (generation == refreshGeneration.get()) {
                refreshState.postValue(result);
            }
        });
    }

    private void publish() {
        RefreshState refresh = refreshState.getValue();
        uiState.setValue(new DetailsUiState(key, details, entry, refresh.refreshing(), refresh.error(), !isOnline));
    }
}

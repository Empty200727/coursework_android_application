package ru.kinopolka.feature.genre;

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
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.LiveDataUtils;
import ru.kinopolka.core.data.util.NetworkMonitor;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.ui.SavedStateValues;
import ru.kinopolka.navigation.Navigator;

/** Genre screen (F-06): paged grid with sorting; the sort order lives in {@link SavedStateHandle}. */
@HiltViewModel
public class GenreViewModel extends ViewModel {

    static final String SORT_KEY = "sort";

    private record Request(Genre genre, MediaSort sort) {
    }

    private final SavedStateHandle savedStateHandle;
    private final MediaFilter filter;
    private final LiveData<PagingData<Media>> media;
    private final MediatorLiveData<GenreUiState> uiState = new MediatorLiveData<>();

    private Genre genre;
    private MediaSort sort;
    private boolean online = true;

    @Inject
    public GenreViewModel(SavedStateHandle savedStateHandle, MediaRepository mediaRepository,
            GenreRepository genreRepository, NetworkMonitor networkMonitor, AppExecutors executors) {
        this.savedStateHandle = savedStateHandle;
        String genreKey = Objects.requireNonNull(savedStateHandle.get(Navigator.ARG_GENRE_KEY));
        filter = SavedStateValues.get(savedStateHandle, Navigator.ARG_FILTER, MediaFilter.ALL);
        sort = SavedStateValues.get(savedStateHandle, SORT_KEY, MediaSort.POPULARITY);
        uiState.setValue(new GenreUiState(null, filter, sort, false));

        LiveData<Genre> genreData = Transformations.distinctUntilChanged(
                Transformations.map(genreRepository.observeGenres(), genres -> find(genres, genreKey)));
        LiveData<MediaSort> sortData = SavedStateValues.enumLiveData(savedStateHandle, SORT_KEY, MediaSort.POPULARITY);

        LiveData<Request> requests = Transformations.distinctUntilChanged(
                LiveDataUtils.combine(genreData, sortData, Request::new));
        media = PagingLiveData.cachedIn(Transformations.switchMap(requests, request -> request.genre() == null
                ? new MutableLiveData<>()
                : mediaRepository.genreMedia(request.genre(), filter, request.sort())),
                ViewModelKt.getViewModelScope(this));

        uiState.addSource(genreData, value -> {
            genre = value;
            publish();
        });
        uiState.addSource(sortData, value -> {
            sort = value;
            publish();
        });
        uiState.addSource(networkMonitor.isOnline(), value -> {
            online = value;
            publish();
        });

        executors.io().execute(() -> genreRepository.refresh(false));
    }

    public LiveData<GenreUiState> getUiState() {
        return uiState;
    }

    public LiveData<PagingData<Media>> getMedia() {
        return media;
    }

    public void onSortChange(MediaSort newSort) {
        savedStateHandle.set(SORT_KEY, newSort.name());
    }

    private void publish() {
        uiState.setValue(new GenreUiState(genre == null ? null : genre.name(), filter, sort, !online));
    }

    private static Genre find(List<Genre> genres, String key) {
        for (Genre genre : genres) {
            if (genre.key().equals(key)) {
                return genre;
            }
        }
        return null;
    }
}

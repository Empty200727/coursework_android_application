package ru.kinopolka.feature.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import ru.kinopolka.core.data.library.LibraryRules;
import ru.kinopolka.core.data.repository.LibraryRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.ui.SavedStateValues;
import ru.kinopolka.feature.library.LibraryUiState.PendingUndo;

/**
 * «Моя полка» (F-12, F-13): reads only the local database, so it works offline (N-01).
 * Tab, sort and filter live in {@link SavedStateHandle}.
 */
@HiltViewModel
public class LibraryViewModel extends ViewModel {

    static final String TAB_KEY = "tab";
    static final String SORT_KEY = "sort";
    static final String FILTER_KEY = "filter";

    private final SavedStateHandle savedStateHandle;
    private final LibraryRepository libraryRepository;
    private final AppExecutors executors;
    private final MutableLiveData<PendingUndo> pendingUndo = new MutableLiveData<>(null);
    private final MediatorLiveData<LibraryUiState> uiState = new MediatorLiveData<>();

    private LibraryTab tab;
    private LibrarySort sort;
    private MediaFilter filter;
    private List<LibraryItem> items;

    @Inject
    public LibraryViewModel(SavedStateHandle savedStateHandle, LibraryRepository libraryRepository,
            AppExecutors executors) {
        this.savedStateHandle = savedStateHandle;
        this.libraryRepository = libraryRepository;
        this.executors = executors;
        tab = SavedStateValues.get(savedStateHandle, TAB_KEY, LibraryTab.WANT);
        sort = SavedStateValues.get(savedStateHandle, SORT_KEY, LibrarySort.ADDED);
        filter = SavedStateValues.get(savedStateHandle, FILTER_KEY, MediaFilter.ALL);
        uiState.setValue(new LibraryUiState(tab, sort, filter, List.of(), Map.of(), true, null));

        uiState.addSource(SavedStateValues.enumLiveData(savedStateHandle, TAB_KEY, LibraryTab.WANT), value -> {
            tab = value;
            publish();
        });
        uiState.addSource(SavedStateValues.enumLiveData(savedStateHandle, SORT_KEY, LibrarySort.ADDED), value -> {
            sort = value;
            publish();
        });
        uiState.addSource(SavedStateValues.enumLiveData(savedStateHandle, FILTER_KEY, MediaFilter.ALL), value -> {
            filter = value;
            publish();
        });
        uiState.addSource(libraryRepository.observeItems(), value -> {
            items = value;
            publish();
        });
        uiState.addSource(pendingUndo, value -> publish());
    }

    public LiveData<LibraryUiState> getUiState() {
        return uiState;
    }

    public void onTabChange(LibraryTab newTab) {
        savedStateHandle.set(TAB_KEY, newTab.name());
    }

    public void onSortChange(LibrarySort newSort) {
        savedStateHandle.set(SORT_KEY, newSort.name());
    }

    public void onFilterChange(MediaFilter newFilter) {
        savedStateHandle.set(FILTER_KEY, newFilter.name());
    }

    /** Removes the title from the current tab; «Отменить» brings it back. */
    public void onRemove(LibraryItem item) {
        LibraryTab currentTab = tab;
        // Only the last removal can be undone: the previous one becomes final.
        PendingUndo previousUndo = pendingUndo.getValue();
        pendingUndo.setValue(null);
        executors.io().execute(() -> {
            if (previousUndo != null) {
                libraryRepository.releasePoster(previousUndo.entry());
            }
            LibraryEntry previous = libraryRepository.removeFromTab(item.entry().key(), currentTab);
            if (previous != null) {
                pendingUndo.postValue(new PendingUndo(previous, item.media().title(), currentTab));
            }
        });
    }

    public void onUndo() {
        PendingUndo undo = pendingUndo.getValue();
        if (undo == null) {
            return;
        }
        pendingUndo.setValue(null);
        executors.io().execute(() -> libraryRepository.restore(undo.entry()));
    }

    public void onUndoDismissed() {
        PendingUndo undo = pendingUndo.getValue();
        if (undo == null) {
            return;
        }
        pendingUndo.setValue(null);
        executors.io().execute(() -> libraryRepository.releasePoster(undo.entry()));
    }

    private void publish() {
        if (items == null) {
            uiState.setValue(new LibraryUiState(tab, sort, filter, List.of(), Map.of(), true, pendingUndo.getValue()));
            return;
        }
        uiState.setValue(new LibraryUiState(tab, sort, filter, LibraryRules.forTab(items, tab, filter, sort),
                LibraryRules.tabCounts(items, filter), false, pendingUndo.getValue()));
    }
}

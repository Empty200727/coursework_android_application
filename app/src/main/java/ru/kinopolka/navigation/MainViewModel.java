package ru.kinopolka.navigation;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** State shared by the screens of the activity. */
@HiltViewModel
public class MainViewModel extends ViewModel {

    private static final String FOCUS_SEARCH_KEY = "focusSearch";

    private final SavedStateHandle savedStateHandle;

    @Inject
    public MainViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
    }

    /** Set by the search field of the home screen: the search screen then focuses its input. */
    public void requestSearchFocus() {
        savedStateHandle.set(FOCUS_SEARCH_KEY, true);
    }

    /** Returns {@code true} once after {@link #requestSearchFocus()}. */
    public boolean consumeSearchFocus() {
        boolean requested = Boolean.TRUE.equals(savedStateHandle.get(FOCUS_SEARCH_KEY));
        savedStateHandle.set(FOCUS_SEARCH_KEY, false);
        return requested;
    }
}

package ru.kinopolka.feature.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.paging.PagingData;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.testing.FakeGenreRepository;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.FakeMediaRepository.Search;
import ru.kinopolka.testing.FakeNetworkMonitor;
import ru.kinopolka.testing.ViewModelTest;

/** F-01, F-02, N-05: debounce, minimal length, cancellation, filter and saved state. */
@RunWith(AndroidJUnit4.class)
public class SearchViewModelTest extends ViewModelTest {

    private final FakeMediaRepository mediaRepository = new FakeMediaRepository();
    private final FakeGenreRepository genreRepository = new FakeGenreRepository();
    private final FakeNetworkMonitor networkMonitor = new FakeNetworkMonitor();

    private SearchViewModel createViewModel(SavedStateHandle savedState) {
        SearchViewModel viewModel = new SearchViewModel(savedState, mediaRepository, genreRepository,
                networkMonitor, AppExecutors.direct());
        observe(viewModel.getResults());
        observe(viewModel.getUiState());
        runCurrent();
        return viewModel;
    }

    private SearchViewModel createViewModel() {
        return createViewModel(new SavedStateHandle());
    }

    @Test
    public void requestGoesOut400MsAfterTypingStops() {
        SearchViewModel viewModel = createViewModel();

        viewModel.onQueryChange("ма");
        advanceTimeBy(SearchUiState.DEBOUNCE_MILLIS - 1);
        assertTrue(mediaRepository.searches.isEmpty());

        advanceTimeBy(2);
        assertEquals(List.of(new Search("ма", MediaFilter.ALL)), mediaRepository.searches);
    }

    @Test
    public void onlyTheLastQueryOfFastTypingIsSearched() {
        SearchViewModel viewModel = createViewModel();

        for (String query : List.of("м", "ма", "мат", "матрица")) {
            viewModel.onQueryChange(query);
            advanceTimeBy(100);
        }
        advanceTimeBy(SearchUiState.DEBOUNCE_MILLIS);

        assertEquals(List.of(new Search("матрица", MediaFilter.ALL)), mediaRepository.searches);
    }

    @Test
    public void queriesShorterThanTwoCharactersAreNotSent() {
        SearchViewModel viewModel = createViewModel();

        viewModel.onQueryChange("м");
        advanceTimeBy(1_000);
        viewModel.onQueryChange("  д  ");
        advanceTimeBy(1_000);

        assertTrue(mediaRepository.searches.isEmpty());
        assertTrue(viewModel.getUiState().getValue().isQueryTooShort());
    }

    @Test
    public void queryIsTrimmedAndARepeatedQueryIsNotSentTwice() {
        SearchViewModel viewModel = createViewModel();

        viewModel.onQueryChange("дюна ");
        advanceTimeBy(500);
        viewModel.onQueryChange("дюна");
        advanceTimeBy(500);

        assertEquals(List.of(new Search("дюна", MediaFilter.ALL)), mediaRepository.searches);
    }

    @Test
    public void newQueryCancelsThePreviousRequest() {
        List<String> cancelled = new ArrayList<>();
        mediaRepository.searchResults = (query, filter) -> new LiveData<PagingData<Media>>(PagingData.empty()) {
            @Override
            protected void onInactive() {
                cancelled.add(query);
            }
        };
        SearchViewModel viewModel = createViewModel();

        viewModel.onQueryChange("дюна");
        advanceTimeBy(500);
        viewModel.onQueryChange("матрица");
        advanceTimeBy(500);

        assertEquals(List.of("дюна", "матрица"), mediaRepository.searches.stream().map(Search::query).toList());
        assertEquals(List.of("дюна"), cancelled);
    }

    @Test
    public void filterChangeSearchesAgainWithoutWaiting() {
        SearchViewModel viewModel = createViewModel();
        viewModel.onQueryChange("тяжкие");
        advanceTimeBy(500);

        viewModel.onFilterChange(MediaFilter.SERIES);
        runCurrent();

        assertEquals(List.of(new Search("тяжкие", MediaFilter.ALL), new Search("тяжкие", MediaFilter.SERIES)),
                mediaRepository.searches);
    }

    @Test
    public void queryAndFilterAreRestoredFromTheSavedState() {
        SearchViewModel viewModel = createViewModel(
                new SavedStateHandle(Map.of("query", "дюна", "filter", MediaFilter.MOVIES.name())));

        SearchUiState state = viewModel.getUiState().getValue();
        assertEquals("дюна", state.query());
        assertEquals(MediaFilter.MOVIES, state.filter());
        advanceTimeBy(500);
        assertEquals(List.of(new Search("дюна", MediaFilter.MOVIES)), mediaRepository.searches);
    }

    @Test
    public void changesAreWrittenToTheSavedState() {
        SavedStateHandle savedState = new SavedStateHandle();
        SearchViewModel viewModel = createViewModel(savedState);

        viewModel.onQueryChange("сол");
        viewModel.onFilterChange(MediaFilter.SERIES);

        assertEquals("сол", savedState.get("query"));
        assertEquals(MediaFilter.SERIES.name(), savedState.get("filter"));
    }

    @Test
    public void offlineStateIsShown() {
        SearchViewModel viewModel = createViewModel();
        assertFalse(viewModel.getUiState().getValue().offline());

        networkMonitor.online.setValue(false);

        assertTrue(viewModel.getUiState().getValue().offline());
    }

    @Test
    public void resultsArePendingWhileTheDebounceRuns() {
        SearchViewModel viewModel = createViewModel();

        viewModel.onQueryChange("дюна");
        runCurrent();
        assertTrue(viewModel.getUiState().getValue().isPending());

        advanceTimeBy(SearchUiState.DEBOUNCE_MILLIS + 1);
        assertFalse(viewModel.getUiState().getValue().isPending());
    }

    @Test
    public void genreCatalogIsLoadedForResultRows() {
        createViewModel();

        assertEquals(1, genreRepository.refreshCalls.get());
    }
}

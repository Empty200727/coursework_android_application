package ru.kinopolka.feature.genre;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.navigation.Navigator;
import ru.kinopolka.testing.FakeGenreRepository;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.FakeNetworkMonitor;
import ru.kinopolka.testing.TestData;
import ru.kinopolka.testing.ViewModelTest;

/** F-06: the genre comes from the arguments, the sort order is saved. */
@RunWith(AndroidJUnit4.class)
public class GenreViewModelTest extends ViewModelTest {

    private final FakeGenreRepository genreRepository = new FakeGenreRepository();
    private final FakeNetworkMonitor networkMonitor = new FakeNetworkMonitor();
    private final SavedStateHandle savedState = new SavedStateHandle(Map.of(Navigator.ARG_GENRE_KEY, "drama",
            Navigator.ARG_FILTER, MediaFilter.SERIES.name()));

    private GenreViewModel createViewModel() {
        GenreViewModel viewModel = new GenreViewModel(savedState, new FakeMediaRepository(), genreRepository,
                networkMonitor, AppExecutors.direct());
        observe(viewModel.getUiState());
        observe(viewModel.getMedia());
        runCurrent();
        return viewModel;
    }

    @Test
    public void titleAppearsOnceTheGenreIsInTheCatalog() {
        GenreViewModel viewModel = createViewModel();
        assertNull(viewModel.getUiState().getValue().title());
        assertNull("no list without a genre", viewModel.getMedia().getValue());

        genreRepository.genres.setValue(List.of(TestData.ACTION, TestData.DRAMA));

        GenreUiState state = viewModel.getUiState().getValue();
        assertEquals("Драма", state.title());
        assertEquals(MediaFilter.SERIES, state.filter());
        assertEquals(1, genreRepository.refreshCalls.get());
    }

    @Test
    public void sortIsAppliedAndSaved() {
        GenreViewModel viewModel = createViewModel();
        assertEquals(MediaSort.POPULARITY, viewModel.getUiState().getValue().sort());

        viewModel.onSortChange(MediaSort.NEWEST);

        assertEquals(MediaSort.NEWEST, viewModel.getUiState().getValue().sort());
        assertEquals(MediaSort.NEWEST.name(), savedState.get(GenreViewModel.SORT_KEY));
    }

    @Test
    public void offlineIsShown() {
        GenreViewModel viewModel = createViewModel();

        networkMonitor.online.setValue(false);

        assertTrue(viewModel.getUiState().getValue().offline());
    }
}

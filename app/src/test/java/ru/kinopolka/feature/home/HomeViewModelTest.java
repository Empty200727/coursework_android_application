package ru.kinopolka.feature.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.TestData.testMedia;

import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.testing.FakeGenreRepository;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.FakeNetworkMonitor;
import ru.kinopolka.testing.TestData;
import ru.kinopolka.testing.ViewModelTest;

@RunWith(AndroidJUnit4.class)
public class HomeViewModelTest extends ViewModelTest {

    private final Genre comedy = new Genre("comedy", "Комедия", 35, 35);
    private final Genre horror = TestData.HORROR;

    private final FakeGenreRepository genreRepository = new FakeGenreRepository();
    private final FakeMediaRepository mediaRepository = new FakeMediaRepository();
    private final FakeNetworkMonitor networkMonitor = new FakeNetworkMonitor();
    private final SavedStateHandle savedState = new SavedStateHandle();

    private HomeViewModel createViewModel(AppExecutors executors) {
        genreRepository.genres.setValue(List.of(comedy, horror));
        HomeViewModel viewModel = new HomeViewModel(savedState, mediaRepository, genreRepository, networkMonitor,
                executors);
        observe(viewModel.getUiState());
        runCurrent();
        return viewModel;
    }

    private HomeViewModel createViewModel() {
        return createViewModel(AppExecutors.direct());
    }

    private HomeUiState state(HomeViewModel viewModel) {
        runCurrent();
        return viewModel.getUiState().getValue();
    }

    private void fillShelves() {
        mediaRepository.onRefresh = shelf -> mediaRepository.setShelf(shelf, List.of(testMedia(1)));
    }

    private static List<Shelf> shelves(HomeUiState state) {
        return state.shelves().stream().map(HomeUiState.ShelfContent::shelf).toList();
    }

    @Test
    public void showsLoadingUntilShelvesAreRefreshed() {
        genreRepository.nextResult = new CompletableFuture<>();
        fillShelves();
        HomeViewModel viewModel = createViewModel(new AppExecutors(Executors.newSingleThreadExecutor()));

        assertEquals(HomeUiState.Content.LOADING, state(viewModel).content());

        genreRepository.nextResult.complete(RefreshResult.SKIPPED);
        waitUntil(() -> !state(viewModel).refreshing());

        HomeUiState state = state(viewModel);
        assertEquals(HomeUiState.Content.DATA, state.content());
        assertEquals(List.of(
                new Shelf.Trending(MediaFilter.ALL),
                new Shelf.Popular(MediaType.MOVIE),
                new Shelf.Popular(MediaType.TV),
                new Shelf.ByGenre(comedy, MediaFilter.ALL),
                new Shelf.ByGenre(horror, MediaFilter.ALL)), shelves(state));
    }

    @Test
    public void everyShelfIsRefreshed() {
        createViewModel();

        assertEquals(5, mediaRepository.refreshedShelves.size());
        assertEquals(1, genreRepository.refreshCalls.get());
    }

    @Test
    public void filterChangesTheShelvesAndIsSaved() {
        fillShelves();
        HomeViewModel viewModel = createViewModel();

        viewModel.onFilterChange(MediaFilter.SERIES);

        HomeUiState state = state(viewModel);
        assertEquals(MediaFilter.SERIES, state.filter());
        assertEquals(List.of(
                new Shelf.Trending(MediaFilter.SERIES),
                new Shelf.Popular(MediaType.TV),
                new Shelf.ByGenre(comedy, MediaFilter.SERIES)), shelves(state));
        assertEquals(MediaFilter.SERIES.name(), savedState.get("filter"));
    }

    @Test
    public void filterIsRestoredFromTheSavedState() {
        savedState.set("filter", MediaFilter.MOVIES.name());

        HomeViewModel viewModel = createViewModel();

        assertEquals(MediaFilter.MOVIES, state(viewModel).filter());
    }

    @Test
    public void nothingCachedAndRefreshFailedShowsTheError() {
        mediaRepository.refreshResult = new RefreshResult.Failed(DataError.SERVER);
        HomeViewModel viewModel = createViewModel();

        assertEquals(HomeUiState.Content.ERROR, state(viewModel).content());
        assertEquals(DataError.SERVER, state(viewModel).error());
    }

    @Test
    public void nothingCachedAndNothingFailedIsEmpty() {
        HomeViewModel viewModel = createViewModel();

        assertEquals(HomeUiState.Content.EMPTY, state(viewModel).content());
    }

    @Test
    public void offlineShowsTheCacheWithTheBannerAndRefreshesWhenTheConnectionReturns() {
        networkMonitor.online.setValue(false);
        mediaRepository.setShelf(new Shelf.Trending(MediaFilter.ALL), List.of(testMedia(7)));
        HomeViewModel viewModel = createViewModel();

        HomeUiState offline = state(viewModel);
        assertTrue(offline.offline());
        assertEquals(HomeUiState.Content.DATA, offline.content());
        assertEquals(DataError.NO_CONNECTION, offline.error());
        assertTrue("no requests without network", mediaRepository.refreshedShelves.isEmpty());

        networkMonitor.online.setValue(true);

        assertFalse(state(viewModel).offline());
        assertEquals(5, mediaRepository.refreshedShelves.size());
    }

    @Test
    public void retryRefreshesAgain() {
        mediaRepository.refreshResult = new RefreshResult.Failed(DataError.NO_CONNECTION);
        HomeViewModel viewModel = createViewModel();
        assertEquals(HomeUiState.Content.ERROR, state(viewModel).content());

        mediaRepository.refreshResult = RefreshResult.UPDATED;
        fillShelves();
        viewModel.retry();

        assertEquals(HomeUiState.Content.DATA, state(viewModel).content());
        assertEquals(10, mediaRepository.refreshedShelves.size());
    }
}

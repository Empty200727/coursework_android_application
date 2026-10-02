package ru.kinopolka.feature.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.TestData.testMedia;

import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.FakeLibraryRepository;
import ru.kinopolka.testing.ViewModelTest;

/** F-12, F-13: tabs, counters, filter and removal with «Отменить». */
@RunWith(AndroidJUnit4.class)
public class LibraryViewModelTest extends ViewModelTest {

    private final FakeLibraryRepository repository = new FakeLibraryRepository();
    private final SavedStateHandle savedState = new SavedStateHandle();
    private final Media matrix = testMedia(603).withTitle("Матрица");
    private final Media arcane = testMedia(94605, MediaType.TV).withTitle("Аркейн");

    private void fill() {
        repository.putMedia(matrix);
        repository.putMedia(arcane);
        repository.setWatchStatus(matrix.key(), WatchStatus.WANT);
        repository.setWatchStatus(arcane.key(), WatchStatus.WANT);
        repository.setFavorite(arcane.key(), true);
    }

    private LibraryViewModel createViewModel() {
        LibraryViewModel viewModel = new LibraryViewModel(savedState, repository, AppExecutors.direct());
        observe(viewModel.getUiState());
        runCurrent();
        return viewModel;
    }

    private static LibraryUiState state(LibraryViewModel viewModel) {
        runCurrent();
        return viewModel.getUiState().getValue();
    }

    private static List<MediaKey> ids(LibraryViewModel viewModel) {
        return state(viewModel).items().stream().map(it -> it.media().key()).toList();
    }

    private static LibraryItem item(LibraryViewModel viewModel, MediaKey key) {
        return state(viewModel).items().stream().filter(it -> it.media().key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    public void tabsShowTheirTitlesWithCounters() {
        fill();
        LibraryViewModel viewModel = createViewModel();

        assertEquals(LibraryTab.WANT, state(viewModel).tab());
        assertEquals(Map.of(LibraryTab.WANT, 2, LibraryTab.WATCHED, 0, LibraryTab.FAVORITES, 1),
                state(viewModel).counts());

        viewModel.onTabChange(LibraryTab.FAVORITES);
        assertEquals(List.of(arcane.key()), ids(viewModel));
        assertEquals(LibraryTab.FAVORITES.name(), savedState.get("tab"));
    }

    @Test
    public void sortAndTypeFilterAreAppliedAndSaved() {
        fill();
        LibraryViewModel viewModel = createViewModel();

        viewModel.onSortChange(LibrarySort.TITLE);
        assertEquals(List.of(arcane.key(), matrix.key()), ids(viewModel));

        viewModel.onFilterChange(MediaFilter.MOVIES);
        assertEquals(List.of(matrix.key()), ids(viewModel));
        assertEquals(1, state(viewModel).count(LibraryTab.WANT));
        assertEquals(LibrarySort.TITLE.name(), savedState.get("sort"));
        assertEquals(MediaFilter.MOVIES.name(), savedState.get("filter"));
    }

    @Test
    public void removalCanBeUndone() {
        fill();
        LibraryViewModel viewModel = createViewModel();

        viewModel.onRemove(item(viewModel, matrix.key()));
        assertEquals(List.of(arcane.key()), ids(viewModel));
        assertEquals("Матрица", state(viewModel).pendingUndo().title());

        viewModel.onUndo();
        assertNull(state(viewModel).pendingUndo());
        assertEquals(Set.of(matrix.key(), arcane.key()), new HashSet<>(ids(viewModel)));
        assertTrue(repository.releasedPosters.isEmpty());
    }

    @Test
    public void removalBecomesFinalWhenTheSnackbarIsDismissed() {
        fill();
        LibraryViewModel viewModel = createViewModel();

        viewModel.onRemove(item(viewModel, matrix.key()));
        viewModel.onUndoDismissed();

        assertNull(repository.entry(matrix.key()));
        assertEquals(List.of(matrix.key()), repository.releasedPosters.stream().map(it -> it.key()).toList());
    }

    @Test
    public void aNewRemovalMakesThePreviousOneFinal() {
        fill();
        LibraryViewModel viewModel = createViewModel();

        viewModel.onRemove(item(viewModel, matrix.key()));
        viewModel.onRemove(item(viewModel, arcane.key()));

        assertEquals("Аркейн", state(viewModel).pendingUndo().title());
        assertEquals(List.of(matrix.key()), repository.releasedPosters.stream().map(it -> it.key()).toList());
    }

    @Test
    public void removalFromFavoritesKeepsTheStatus() {
        fill();
        LibraryViewModel viewModel = createViewModel();
        viewModel.onTabChange(LibraryTab.FAVORITES);

        viewModel.onRemove(state(viewModel).items().get(0));

        assertTrue(state(viewModel).items().isEmpty());
        assertEquals(WatchStatus.WANT, repository.entry(new MediaKey(MediaType.TV, 94605)).watchStatus());
    }
}

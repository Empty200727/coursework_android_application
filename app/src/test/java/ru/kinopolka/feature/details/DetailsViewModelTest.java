package ru.kinopolka.feature.details;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.kinopolka.testing.TestData.testMedia;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.navigation.Navigator;
import ru.kinopolka.testing.FakeDetailsRepository;
import ru.kinopolka.testing.FakeLibraryRepository;
import ru.kinopolka.testing.FakeNetworkMonitor;
import ru.kinopolka.testing.ViewModelTest;

/** F-11: the card changes the library; offline it shows the cache. */
@RunWith(AndroidJUnit4.class)
@Config(qualifiers = "ru")
public class DetailsViewModelTest extends ViewModelTest {

    private final MediaKey key = new MediaKey(MediaType.MOVIE, 550);
    private final FakeDetailsRepository detailsRepository = new FakeDetailsRepository();
    private final FakeLibraryRepository libraryRepository = new FakeLibraryRepository();
    private final FakeNetworkMonitor networkMonitor = new FakeNetworkMonitor();

    private final MediaDetails details = details(testMedia(550, MediaType.MOVIE, 1.0, 8.4,
            LocalDate.of(1999, 10, 15)), 139, null, null, null);

    private static MediaDetails details(Media media, Integer runtime, Integer seasons, LocalDate lastAirDate,
            Boolean inProduction) {
        return new MediaDetails(media, List.of(), runtime, seasons, lastAirDate, inProduction, false, List.of(),
                List.of(), List.of(), true);
    }

    private DetailsViewModel createViewModel() {
        SavedStateHandle savedState = new SavedStateHandle(Map.of(Navigator.ARG_MEDIA_TYPE, "movie",
                Navigator.ARG_ID, 550));
        DetailsViewModel viewModel = new DetailsViewModel(savedState, detailsRepository, libraryRepository,
                networkMonitor, AppExecutors.direct());
        observe(viewModel.getUiState());
        runCurrent();
        return viewModel;
    }

    private DetailsUiState state(DetailsViewModel viewModel) {
        runCurrent();
        return viewModel.getUiState().getValue();
    }

    @Test
    public void argumentsSelectTheTitleAndTheCardIsRefreshed() {
        detailsRepository.setDetails(details);
        DetailsViewModel viewModel = createViewModel();

        assertEquals(key, state(viewModel).key());
        assertEquals(DetailsUiState.Content.DATA, state(viewModel).content());
        assertEquals(List.of(key), detailsRepository.refreshed);
    }

    @Test
    public void wantAndWatchedExcludeEachOtherAndASecondTapClearsTheStatus() {
        detailsRepository.setDetails(details);
        DetailsViewModel viewModel = createViewModel();

        viewModel.onStatusClick(WatchStatus.WANT);
        assertEquals(WatchStatus.WANT, state(viewModel).watchStatus());

        viewModel.onStatusClick(WatchStatus.WATCHED);
        assertEquals(WatchStatus.WATCHED, state(viewModel).watchStatus());

        viewModel.onStatusClick(WatchStatus.WATCHED);
        assertNull("no status and no favorite: entry removed", state(viewModel).entry());
    }

    @Test
    public void favoriteIsToggledIndependently() {
        detailsRepository.setDetails(details);
        DetailsViewModel viewModel = createViewModel();
        viewModel.onStatusClick(WatchStatus.WATCHED);

        viewModel.onFavoriteClick();
        assertTrue(state(viewModel).favorite());
        assertEquals(WatchStatus.WATCHED, state(viewModel).watchStatus());

        viewModel.onFavoriteClick();
        assertFalse(state(viewModel).favorite());
    }

    @Test
    public void offlineTheCachedCardIsShownWithoutARequest() {
        networkMonitor.online.setValue(false);
        detailsRepository.setDetails(details);
        DetailsViewModel viewModel = createViewModel();

        DetailsUiState state = state(viewModel);
        assertTrue(state.offline());
        assertEquals(DetailsUiState.Content.DATA, state.content());
        assertTrue(detailsRepository.refreshed.isEmpty());
    }

    @Test
    public void nothingCachedAndLoadingFailedShowsTheErrorAndRetryWorks() {
        detailsRepository.refreshResult = new RefreshResult.Failed(DataError.SERVER);
        DetailsViewModel viewModel = createViewModel();
        assertEquals(DetailsUiState.Content.ERROR, state(viewModel).content());
        assertEquals(DataError.SERVER, state(viewModel).error());

        detailsRepository.refreshResult = RefreshResult.UPDATED;
        detailsRepository.setDetails(details);
        viewModel.retry();

        assertEquals(DetailsUiState.Content.DATA, state(viewModel).content());
        assertEquals(2, detailsRepository.refreshed.size());
    }

    @Test
    public void yearsRuntimeAndVotesAreFormatted() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals("1999", DetailsFormat.yearsText(details, "н. в."));
        MediaDetails series = details(testMedia(1, MediaType.TV, 1.0, 8.9, LocalDate.of(2008, 1, 20)), null, 5,
                LocalDate.of(2013, 9, 29), false);
        assertEquals("2008–2013", DetailsFormat.yearsText(series, "н. в."));
        assertEquals("2008–н. в.", DetailsFormat.yearsText(details(series.media(), null, 5, null, true), "н. в."));

        assertEquals("1999 · Фильм · 2 ч 19 мин", DetailsFragment.metaLine(context, details));
        assertEquals("2008–2013 · Сериал · 5 сезонов", DetailsFragment.metaLine(context, series));
        assertEquals("★ 8.4 · 500 оценок", DetailsFragment.ratingLine(context, details.media()));
        assertEquals("29 870", DetailsFormat.formatCount(29_870).replace(' ', ' '));
    }
}

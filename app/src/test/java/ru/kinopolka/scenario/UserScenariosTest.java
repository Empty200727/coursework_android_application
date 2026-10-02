package ru.kinopolka.scenario;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ru.kinopolka.R;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.FakeMediaRepository.Search;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.UiTest;

/**
 * End-to-end scenarios of docs/PLAN.md, section 8, on the real activity and navigation with fake
 * repositories: search → card → «Хочу посмотреть»; «Моя полка» without network; removal with «Отменить».
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, qualifiers = "ru-w411dp-h891dp")
public class UserScenariosTest extends UiTest {

    @Before
    public void fillFakes() {
        mediaRepository.searchResults =
                (query, filter) -> FakeMediaRepository.pages(List.of(ru.kinopolka.testing.UiData.MATRIX));
        detailsRepository.setDetails(ru.kinopolka.testing.UiData.MATRIX_DETAILS);
        libraryRepository.putMedia(ru.kinopolka.testing.UiData.MATRIX);
    }

    @Test
    public void searchOpenCardAndAddToWantList() {
        launch();
        onView(withText("Поиск фильмов и сериалов")).perform(click());
        onView(withId(R.id.search_input)).perform(replaceText("матрица"));
        waitForDebounce();

        onView(withText("1999 · Фильм")).perform(click());
        onView(withText("The Matrix")).check(matches(isDisplayed()));
        onView(withText("Хочу посмотреть")).perform(click());

        assertEquals(WatchStatus.WANT,
                libraryRepository.entry(ru.kinopolka.testing.UiData.MATRIX.key()).watchStatus());
        assertEquals(List.of("матрица"), mediaRepository.searches.stream().map(Search::query).toList());

        onView(withContentDescription("Назад")).perform(click());
        onView(withId(R.id.library_graph)).perform(click());
        onView(withText("Хочу (1)")).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.title), withText("Матрица"))).check(matches(isDisplayed()));
    }

    @Test
    public void libraryAndSavedCardWorkOffline() {
        networkMonitor.online.setValue(false);
        libraryRepository.putEntry(new LibraryEntry(ru.kinopolka.testing.UiData.MATRIX.key(), WatchStatus.WATCHED,
                true, Instant.EPOCH, Instant.EPOCH, null, null));
        launch();

        onView(withId(R.id.library_graph)).perform(click());
        onView(withText("Смотрел (1)")).perform(click());
        onView(allOf(withId(R.id.title), withText("Матрица"))).perform(click());

        onView(withText("Нет подключения. Показаны сохранённые данные.")).check(matches(isDisplayed()));
        onView(withText("The Matrix")).check(matches(isDisplayed()));
        onView(withContentDescription("Убрать из избранного")).check(matches(isDisplayed()));
        assertEquals("no network requests offline", List.of(), detailsRepository.refreshed);
    }

    @Test
    public void removeFromLibraryAndUndo() {
        libraryRepository.putEntry(new LibraryEntry(ru.kinopolka.testing.UiData.MATRIX.key(), WatchStatus.WANT,
                false, Instant.EPOCH, null, null, null));
        launch();
        onView(withId(R.id.library_graph)).perform(click());

        onView(withContentDescription("Удалить из списка «Хочу»")).perform(click());
        onView(withText("«Матрица» удалено из списка «Хочу»")).check(matches(isDisplayed()));
        onView(withText("Хочу (0)")).check(matches(isDisplayed()));

        onView(withText("Отменить")).perform(click());

        onView(withText("Хочу (1)")).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.title), withText("Матрица"))).check(matches(isDisplayed()));
        assertEquals(WatchStatus.WANT,
                libraryRepository.entry(ru.kinopolka.testing.UiData.MATRIX.key()).watchStatus());
    }
}

package ru.kinopolka.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.TestData;
import ru.kinopolka.testing.UiData;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.UiTest;

/** Every screen shows its states (N-06) and reacts to the main actions. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, qualifiers = "ru-w411dp-h891dp")
public class ScreensSmokeTest extends UiTest {

    private static Matcher<android.view.View> title(String text) {
        return allOf(withId(R.id.title), withText(text));
    }

    private void fillHome() {
        genreRepository.genres.setValue(List.of(TestData.ACTION));
        mediaRepository.setShelf(new Shelf.Trending(MediaFilter.ALL), List.of(UiData.MATRIX));
        mediaRepository.setShelf(new Shelf.ByGenre(TestData.ACTION, MediaFilter.ALL), List.of(UiData.ARCANE));
        detailsRepository.setDetails(UiData.MATRIX_DETAILS);
    }

    @Test
    public void homeShowsShelvesAndOpensGenreAndTitle() {
        fillHome();
        mediaRepository.genreResults = FakeMediaRepository.pages(List.of(UiData.ARCANE));
        launch();

        onView(withText("В тренде за неделю")).check(matches(isDisplayed()));
        onView(title("Матрица")).check(matches(isDisplayed()));
        onView(withId(R.id.home_list)).perform(scrollTo(hasDescendant(withText("Боевик"))));
        onView(withContentDescription("Показать все: Боевик")).perform(click());
        onView(allOf(withText("Боевик"), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
        onView(title("Аркейн")).check(matches(isDisplayed()));

        pressBack();
        onView(withId(R.id.home_list)).perform(scrollTo(hasDescendant(withText("В тренде за неделю"))));
        onView(title("Матрица")).perform(click());
        onView(withText("The Matrix")).check(matches(isDisplayed()));
    }

    @Test
    public void homeFilterChangesTheShelves() {
        fillHome();
        mediaRepository.setShelf(new Shelf.Popular(MediaType.TV), List.of(UiData.ARCANE));
        launch();

        onView(withText("Сериалы")).perform(click());

        onView(withText("Сериалы")).check(matches(isChecked()));
        assertTrue(mediaRepository.refreshedShelves.contains(new Shelf.Trending(MediaFilter.SERIES)));
        onView(withText("Популярные сериалы")).check(matches(isDisplayed()));
    }

    @Test
    public void homeShowsErrorWithRetry() {
        mediaRepository.refreshResult = new RefreshResult.Failed(DataError.SERVER);
        launch();
        int refreshed = mediaRepository.refreshedShelves.size();

        onView(withText("Не удалось загрузить данные")).check(matches(isDisplayed()));
        onView(withText("Повторить")).perform(click());

        assertEquals(refreshed * 2, mediaRepository.refreshedShelves.size());
    }

    @Test
    public void homeWithoutConnectionShowsTheBannerOverTheCache() {
        fillHome();
        networkMonitor.online.setValue(false);
        launch();

        onView(withText("Нет подключения. Показаны сохранённые данные.")).check(matches(isDisplayed()));
        onView(title("Матрица")).check(matches(isDisplayed()));
    }

    @Test
    public void searchRowShowsYearTypeAndGenres() {
        mediaRepository.searchResults = (query, filter) -> FakeMediaRepository.pages(List.of(UiData.MATRIX));
        genreRepository.names.setValue(Map.of(MediaType.MOVIE, Map.of(28, "Боевик")));
        launch();

        onView(withId(R.id.search_graph)).perform(click());
        onView(withId(R.id.search_input)).perform(replaceText("матрица"));
        waitForDebounce();

        onView(title("Матрица")).check(matches(isDisplayed()));
        onView(withText("1999 · Фильм")).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.genres), withText("Боевик"))).check(matches(isDisplayed()));
    }

    @Test
    public void searchAsksForTwoCharactersAndReportsAnEmptyResult() {
        launch();
        onView(withId(R.id.search_graph)).perform(click());

        onView(withId(R.id.search_input)).perform(replaceText("м"));
        waitForDebounce();
        onView(withText("Введите хотя бы два символа названия")).check(matches(isDisplayed()));

        onView(withId(R.id.search_input)).perform(replaceText("мм"));
        waitForDebounce();
        onView(withText("Ничего не найдено. Попробуйте другое название.")).check(matches(isDisplayed()));
    }

    @Test
    public void genreGridShowsPostersAndSortChips() {
        fillHome();
        mediaRepository.genreResults = FakeMediaRepository.pages(List.of(UiData.ARCANE));
        launch();
        onView(withId(R.id.home_list)).perform(scrollTo(hasDescendant(withText("Боевик"))));
        onView(withContentDescription("Показать все: Боевик")).perform(click());

        onView(allOf(withId(R.id.title), withText("Аркейн"), isDescendantOfA(withId(R.id.grid))))
                .check(matches(isDisplayed()));
        onView(withText("Популярные")).check(matches(isChecked()));
        onView(withText("По рейтингу")).perform(click());
        onView(withText("По рейтингу")).check(matches(isChecked()));
    }

    @Test
    public void detailsShowsTheCardAndChangesTheLibrary() {
        fillHome();
        libraryRepository.putMedia(UiData.MATRIX);
        launch();
        onView(title("Матрица")).perform(click());

        onView(allOf(withText("Матрица"), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
        onView(withText("1999 · Фильм · 2 ч 16 мин")).check(matches(isDisplayed()));
        onView(withText("Киану Ривз")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));

        onView(withText("Хочу посмотреть")).perform(click());
        assertEquals(WatchStatus.WANT, libraryRepository.entry(UiData.MATRIX.key()).watchStatus());
        onView(withContentDescription("Добавить в избранное")).perform(click());
        assertTrue(libraryRepository.entry(UiData.MATRIX.key()).favorite());
        onView(withContentDescription("Убрать из избранного")).check(matches(isDisplayed()));
    }

    @Test
    public void detailsWithoutCacheShowsTheErrorWithRetry() {
        mediaRepository.setShelf(new Shelf.Trending(MediaFilter.ALL), List.of(UiData.ARCANE));
        detailsRepository.refreshResult = new RefreshResult.Failed(DataError.UNAUTHORIZED);
        launch();
        onView(title("Аркейн")).perform(click());

        onView(withText("Нет доступа к TMDB. Проверьте TMDB_TOKEN в local.properties.")).check(matches(isDisplayed()));
        onView(withText("Повторить")).perform(click());
        assertEquals(2, detailsRepository.refreshed.size());
    }

    @Test
    public void emptyLibraryOffersToFindSomething() {
        launch();
        onView(withId(R.id.library_graph)).perform(click());

        onView(withText("Добавляйте сюда то, что хотите посмотреть")).check(matches(isDisplayed()));
        onView(withText("Найти, что посмотреть")).perform(click());

        onView(withId(R.id.search_input)).check(matches(hasFocus()));
    }

    @Test
    public void aboutShowsVersionAndTmdbAttribution() {
        launch();
        onView(withId(R.id.library_graph)).perform(click());
        onView(withContentDescription("О приложении")).perform(click());

        onView(withText("Версия 1.0.0")).check(matches(isDisplayed()));
        onView(withContentDescription("Логотип TMDB")).check(matches(isDisplayed()));
        onView(withText("This product uses the TMDB API but is not endorsed or certified by TMDB."))
                .check(matches(isDisplayed()));
        onView(withContentDescription("Назад")).perform(click());
        onView(withText("Добавляйте сюда то, что хотите посмотреть")).check(matches(isDisplayed()));
    }

}

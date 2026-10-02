package ru.kinopolka.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import ru.kinopolka.R;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.testing.FakeMediaRepository;
import ru.kinopolka.testing.TestData;
import ru.kinopolka.testing.UiData;
import ru.kinopolka.testing.UiTest;

/**
 * N-08: every visible clickable element has a touch target of at least 48 dp and a label for
 * TalkBack (text, hint or content description), also with the font enlarged to 150%. Android's
 * Accessibility Test Framework finds nothing under Robolectric, so the checks are made here.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, qualifiers = "ru-w411dp-h891dp")
public class AccessibilityTest extends UiTest {

    private static final int MIN_TOUCH_TARGET_DP = 48;
    private static final float LARGE_FONT = 1.5f;

    private void fill() {
        genreRepository.genres.setValue(List.of(TestData.ACTION));
        genreRepository.names.setValue(Map.of(MediaType.MOVIE, Map.of(28, "Боевик")));
        mediaRepository.setShelf(new Shelf.Trending(MediaFilter.ALL), List.of(UiData.MATRIX));
        mediaRepository.setShelf(new Shelf.ByGenre(TestData.ACTION, MediaFilter.ALL), List.of(UiData.ARCANE));
        mediaRepository.searchResults = (query, filter) -> FakeMediaRepository.pages(List.of(UiData.MATRIX));
        mediaRepository.genreResults = FakeMediaRepository.pages(List.of(UiData.ARCANE));
        detailsRepository.setDetails(UiData.MATRIX_DETAILS);
        libraryRepository.putMedia(UiData.MATRIX);
        libraryRepository.putEntry(new LibraryEntry(UiData.MATRIX.key(), WatchStatus.WANT, true, Instant.EPOCH, null,
                null, null));
    }

    private void start(float fontScale) {
        RuntimeEnvironment.setFontScale(fontScale);
        fill();
        launch();
    }

    private void assertAccessible() {
        List<String> problems = new ArrayList<>();
        scenario.onActivity(activity -> problems.addAll(problems(activity.getWindow().getDecorView())));
        assertEquals(List.of(), problems);
    }

    /** Problems of the visible clickable views under {@code root}. */
    static List<String> problems(View root) {
        List<String> problems = new ArrayList<>();
        collect(root, problems);
        return problems;
    }

    private static void collect(View view, List<String> problems) {
        if (!view.isShown() || view.getWidth() == 0 || view.getHeight() == 0) {
            return;
        }
        if (view.isEnabled() && (view.isClickable() || view.isLongClickable())) {
            float density = view.getResources().getDisplayMetrics().density;
            String name = describe(view);
            if (view.getWidth() < MIN_TOUCH_TARGET_DP * density || view.getHeight() < MIN_TOUCH_TARGET_DP * density) {
                problems.add(name + " is smaller than 48 dp: " + view.getWidth() + "x" + view.getHeight());
            }
            if (label(view).isBlank()) {
                problems.add(name + " has no label");
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), problems);
            }
        }
    }

    /** What TalkBack reads: the content description, otherwise the texts of the view and its children. */
    private static String label(View view) {
        CharSequence description = view.getContentDescription();
        if (description != null && description.length() > 0) {
            return description.toString();
        }
        StringBuilder text = new StringBuilder();
        if (view instanceof TextView textView) {
            text.append(textView.getText());
            if (textView.getText().length() == 0 && textView.getHint() != null) {
                text.append(textView.getHint());
            }
        }
        if (view instanceof ViewGroup group
                && view.getImportantForAccessibility() != View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.isShown()) {
                    text.append(' ').append(label(child));
                }
            }
        }
        return text.toString();
    }

    private static String describe(View view) {
        String id = view.getId() == View.NO_ID ? "" : "#" + view.getResources().getResourceEntryName(view.getId());
        return view.getClass().getSimpleName() + id;
    }

    private void home(float fontScale) {
        start(fontScale);
        assertAccessible();
    }

    @Test
    public void home() {
        home(1f);
    }

    @Test
    public void homeLargeFont() {
        home(LARGE_FONT);
    }

    private void search(float fontScale) {
        start(fontScale);
        onView(withId(R.id.search_graph)).perform(click());
        onView(withId(R.id.search_input)).perform(replaceText("матрица"));
        waitForDebounce();
        assertAccessible();
    }

    @Test
    public void search() {
        search(1f);
    }

    @Test
    public void searchLargeFont() {
        search(LARGE_FONT);
    }

    @Test
    public void genre() {
        start(LARGE_FONT);
        onView(withId(R.id.home_list)).perform(scrollTo(hasDescendant(withText("Боевик"))));
        onView(withContentDescription("Показать все: Боевик")).perform(click());
        assertAccessible();
    }

    private void details(float fontScale) {
        start(fontScale);
        onView(allOf(withId(R.id.title), withText("Матрица"))).perform(click());
        assertAccessible();
    }

    @Test
    public void details() {
        details(1f);
    }

    @Test
    public void detailsLargeFont() {
        details(LARGE_FONT);
    }

    @Test
    public void library() {
        start(LARGE_FONT);
        onView(withId(R.id.library_graph)).perform(click());
        assertAccessible();
    }

    @Test
    public void about() {
        start(LARGE_FONT);
        onView(withId(R.id.library_graph)).perform(click());
        onView(withContentDescription("О приложении")).perform(click());
        assertAccessible();
    }

    @Test
    public void checksDetectProblems() {
        launch();
        List<String> problems = new ArrayList<>();
        scenario.onActivity(activity -> {
            View tiny = new View(activity);
            tiny.setOnClickListener(v -> { });
            tiny.setId(R.id.root);
            addToContent(activity, tiny);
            problems.addAll(problems(activity.getWindow().getDecorView()));
        });

        assertTrue(problems.toString(), problems.contains("View#root is smaller than 48 dp: 20x20"));
        assertTrue(problems.toString(), problems.contains("View#root has no label"));
    }

    private static void addToContent(Activity activity, View view) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        content.addView(view, new FrameLayout.LayoutParams(20, 20));
        content.measure(View.MeasureSpec.makeMeasureSpec(content.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(content.getHeight(), View.MeasureSpec.EXACTLY));
        content.layout(content.getLeft(), content.getTop(), content.getRight(), content.getBottom());
    }
}

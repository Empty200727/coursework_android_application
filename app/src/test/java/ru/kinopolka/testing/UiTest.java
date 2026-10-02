package ru.kinopolka.testing;

import static androidx.test.espresso.Espresso.onIdle;

import android.os.Looper;
import androidx.test.core.app.ActivityScenario;
import dagger.hilt.android.testing.HiltAndroidRule;
import java.time.Duration;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.robolectric.Shadows;
import ru.kinopolka.MainActivity;

/**
 * UI tests on the real activity and navigation with fake repositories (Hilt test module).
 * Subclasses are annotated with {@code @HiltAndroidTest} and {@code @Config(application =
 * HiltTestApplication.class, qualifiers = "ru-w411dp-h891dp")}; they fill the fakes before {@link #launch()}.
 */
public abstract class UiTest {

    @Rule
    public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    public FakeGenreRepository genreRepository;
    @Inject
    public FakeMediaRepository mediaRepository;
    @Inject
    public FakeDetailsRepository detailsRepository;
    @Inject
    public FakeLibraryRepository libraryRepository;
    @Inject
    public FakeNetworkMonitor networkMonitor;

    protected ActivityScenario<MainActivity> scenario;

    @Before
    public void injectFakes() {
        hiltRule.inject();
    }

    protected void launch() {
        scenario = ActivityScenario.launch(MainActivity.class);
        onIdle();
    }

    /** Lets the 400 ms search debounce on the main looper pass. */
    protected static void waitForDebounce() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
        onIdle();
    }

    @After
    public void closeActivity() {
        if (scenario != null) {
            scenario.close();
        }
    }
}

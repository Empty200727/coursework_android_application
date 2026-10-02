package ru.kinopolka.testing;

import android.os.Looper;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Rule;
import org.robolectric.Shadows;

/** Base class of ViewModel tests on Robolectric: LiveData is observed, time of the main looper is controlled. */
public abstract class ViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    private final List<Runnable> cleanups = new ArrayList<>();

    /** Observes {@code data} until the end of the test, like a screen would. */
    protected <T> void observe(LiveData<T> data) {
        Observer<T> observer = value -> { };
        data.observeForever(observer);
        cleanups.add(() -> data.removeObserver(observer));
    }

    protected static void advanceTimeBy(long millis) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));
    }

    protected static void runCurrent() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /** Waits for background work that reports through LiveData. */
    protected static void waitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition not met in time");
            }
            runCurrent();
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    @After
    public void removeObservers() {
        cleanups.forEach(Runnable::run);
    }
}

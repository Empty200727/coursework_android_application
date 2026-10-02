package ru.kinopolka.testing;

import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.robolectric.Shadows;

/** Reading LiveData in tests; the main looper of Robolectric is run while waiting. */
public final class LiveDataTesting {

    private static final long TIMEOUT_MILLIS = 5_000;

    private LiveDataTesting() {
    }

    /** The first value that matches {@code condition}. */
    public static <T> T awaitValue(LiveData<T> liveData, Predicate<T> condition) {
        List<T> values = new ArrayList<>();
        Observer<T> observer = values::add;
        runOnMain(() -> liveData.observeForever(observer));
        try {
            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
            while (System.currentTimeMillis() < deadline) {
                idleMain();
                synchronized (values) {
                    for (T value : values) {
                        if (condition.test(value)) {
                            return value;
                        }
                    }
                }
                sleep();
            }
            throw new AssertionError(new TimeoutException("No matching value; got " + values));
        } finally {
            runOnMain(() -> liveData.removeObserver(observer));
        }
    }

    public static <T> T awaitValue(LiveData<T> liveData) {
        return awaitValue(liveData, value -> true);
    }

    /** Keeps the LiveData active, e.g. to run its sources; returns the observer for removal. */
    public static <T> Observer<T> observe(LiveData<T> liveData) {
        Observer<T> observer = value -> { };
        runOnMain(() -> liveData.observeForever(observer));
        return observer;
    }

    /** Runs pending main thread tasks of Robolectric. */
    public static void idleMain() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }

    private static void runOnMain(Runnable action) {
        action.run();
        idleMain();
    }

    private static void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}

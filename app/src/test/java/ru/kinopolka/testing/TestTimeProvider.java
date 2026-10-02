package ru.kinopolka.testing;

import ru.kinopolka.core.data.TimeProvider;

/** Controllable clock for cache policy tests (N-03). */
public final class TestTimeProvider implements TimeProvider {

    public static final long START = 1_700_000_000_000L;

    private volatile long now;

    public TestTimeProvider() {
        this(START);
    }

    public TestTimeProvider(long now) {
        this.now = now;
    }

    @Override
    public long nowMillis() {
        return now;
    }

    public void setNow(long value) {
        now = value;
    }

    public void advanceBy(long millis) {
        now += millis;
    }
}

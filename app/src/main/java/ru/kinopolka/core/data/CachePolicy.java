package ru.kinopolka.core.data;

import androidx.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/** Cache lifetimes from N-03 and the cleanup rule from docs/PLAN.md, section 6. */
public final class CachePolicy {

    public static final long GENRES = TimeUnit.DAYS.toMillis(7);
    public static final long FEEDS = TimeUnit.HOURS.toMillis(6);
    public static final long DETAILS = TimeUnit.HOURS.toMillis(24);

    /** Cached titles older than this and not in the library are removed daily. */
    public static final long CLEANUP_AGE = TimeUnit.DAYS.toMillis(30);

    private CachePolicy() {
    }

    /**
     * Data is stale when it was never loaded, when {@code ttlMillis} has passed, or when it claims
     * to be from the future (the device clock was moved back).
     */
    public static boolean isStale(@Nullable Long cachedAtMillis, long ttlMillis, long nowMillis) {
        if (cachedAtMillis == null || cachedAtMillis > nowMillis) {
            return true;
        }
        return nowMillis - cachedAtMillis >= ttlMillis;
    }
}

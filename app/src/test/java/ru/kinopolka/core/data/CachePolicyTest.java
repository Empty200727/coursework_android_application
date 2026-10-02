package ru.kinopolka.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import ru.kinopolka.testing.TestTimeProvider;

/** N-03: cache lifetimes with a controllable clock. */
public class CachePolicyTest {

    private final TestTimeProvider clock = new TestTimeProvider();

    @Test
    public void lifetimesMatchTheRequirements() {
        assertEquals(TimeUnit.DAYS.toMillis(7), CachePolicy.GENRES);
        assertEquals(TimeUnit.HOURS.toMillis(6), CachePolicy.FEEDS);
        assertEquals(TimeUnit.HOURS.toMillis(24), CachePolicy.DETAILS);
        assertEquals(TimeUnit.DAYS.toMillis(30), CachePolicy.CLEANUP_AGE);
    }

    @Test
    public void neverLoadedDataIsStale() {
        assertTrue(CachePolicy.isStale(null, CachePolicy.FEEDS, clock.nowMillis()));
    }

    @Test
    public void dataIsFreshUntilTheLifetimePasses() {
        long cachedAt = clock.nowMillis();

        clock.advanceBy(TimeUnit.HOURS.toMillis(6) - 1);
        assertFalse(CachePolicy.isStale(cachedAt, CachePolicy.FEEDS, clock.nowMillis()));

        clock.advanceBy(1);
        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.FEEDS, clock.nowMillis()));
    }

    @Test
    public void genresLiveAWeekAndDetailsADay() {
        long cachedAt = clock.nowMillis();
        clock.advanceBy(TimeUnit.DAYS.toMillis(2));

        assertFalse(CachePolicy.isStale(cachedAt, CachePolicy.GENRES, clock.nowMillis()));
        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.DETAILS, clock.nowMillis()));
    }

    @Test
    public void dataFromTheFutureIsStale() {
        long cachedAt = clock.nowMillis() + TimeUnit.HOURS.toMillis(1);

        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.GENRES, clock.nowMillis()));
    }
}

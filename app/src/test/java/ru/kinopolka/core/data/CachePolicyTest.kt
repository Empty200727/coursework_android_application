package ru.kinopolka.core.data

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kinopolka.testing.TestTimeProvider

/** N-03: cache lifetimes with a controllable clock. */
class CachePolicyTest {

    private val clock = TestTimeProvider()

    @Test
    fun `lifetimes match the requirements`() {
        assertEquals(7.days, CachePolicy.GENRES)
        assertEquals(6.hours, CachePolicy.FEEDS)
        assertEquals(24.hours, CachePolicy.DETAILS)
        assertEquals(30.days, CachePolicy.CLEANUP_AGE)
    }

    @Test
    fun `never loaded data is stale`() {
        assertTrue(CachePolicy.isStale(null, CachePolicy.FEEDS, clock.nowMillis()))
    }

    @Test
    fun `data is fresh until the lifetime passes`() {
        val cachedAt = clock.nowMillis()

        clock.advanceBy(6.hours - 1.milliseconds)
        assertFalse(CachePolicy.isStale(cachedAt, CachePolicy.FEEDS, clock.nowMillis()))

        clock.advanceBy(1.milliseconds)
        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.FEEDS, clock.nowMillis()))
    }

    @Test
    fun `genres live a week and details a day`() {
        val cachedAt = clock.nowMillis()
        clock.advanceBy(2.days)

        assertFalse(CachePolicy.isStale(cachedAt, CachePolicy.GENRES, clock.nowMillis()))
        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.DETAILS, clock.nowMillis()))
    }

    @Test
    fun `data from the future is stale`() {
        val cachedAt = clock.nowMillis() + 1.hours.inWholeMilliseconds

        assertTrue(CachePolicy.isStale(cachedAt, CachePolicy.GENRES, clock.nowMillis()))
    }
}

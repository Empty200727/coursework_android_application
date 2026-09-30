package ru.kinopolka.core.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Cache lifetimes from N-03 and the cleanup rule from docs/PLAN.md, section 6. */
object CachePolicy {
    val GENRES: Duration = 7.days
    val FEEDS: Duration = 6.hours
    val DETAILS: Duration = 24.hours

    /** Cached titles older than this and not in the library are removed daily. */
    val CLEANUP_AGE: Duration = 30.days

    /**
     * Data is stale when it was never loaded, when [ttl] has passed, or when it claims to be
     * from the future (the device clock was moved back).
     */
    fun isStale(cachedAtMillis: Long?, ttl: Duration, nowMillis: Long): Boolean {
        if (cachedAtMillis == null || cachedAtMillis > nowMillis) return true
        return nowMillis - cachedAtMillis >= ttl.inWholeMilliseconds
    }
}

/** Source of the current time, replaced by a fake in tests. */
fun interface TimeProvider {
    fun nowMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

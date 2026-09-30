package ru.kinopolka.testing

import kotlin.time.Duration
import ru.kinopolka.core.data.TimeProvider

/** Controllable clock for cache policy tests (N-03). */
class TestTimeProvider(var now: Long = 1_700_000_000_000L) : TimeProvider {
    override fun nowMillis(): Long = now

    fun advanceBy(duration: Duration) {
        now += duration.inWholeMilliseconds
    }
}

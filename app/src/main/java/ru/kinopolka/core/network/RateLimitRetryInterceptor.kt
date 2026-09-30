package ru.kinopolka.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Repeats a request answered with `429 Too Many Requests` with a growing pause:
 * `Retry-After` when the server sends it, otherwise 1 s, 2 s, 4 s… (docs/PLAN.md, section 4).
 * After [maxRetries] attempts the last 429 response is returned to the caller.
 */
class RateLimitRetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0
        while (response.code == HTTP_TOO_MANY_REQUESTS && attempt < maxRetries) {
            val delay = retryDelayMillis(response.header(RETRY_AFTER_HEADER), attempt)
            response.close()
            sleep(delay)
            attempt++
            response = chain.proceed(request)
        }
        return response
    }

    internal fun retryDelayMillis(retryAfter: String?, attempt: Int): Long {
        val retryAfterMillis = retryAfter?.trim()?.toLongOrNull()?.times(MILLIS_IN_SECOND)
        val backoffMillis = baseDelayMillis shl attempt.coerceAtMost(MAX_SHIFT)
        return (retryAfterMillis ?: backoffMillis).coerceIn(0, maxDelayMillis)
    }

    companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val RETRY_AFTER_HEADER = "Retry-After"
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_BASE_DELAY_MILLIS = 1_000L
        const val DEFAULT_MAX_DELAY_MILLIS = 10_000L
        private const val MILLIS_IN_SECOND = 1_000L
        private const val MAX_SHIFT = 16
    }
}

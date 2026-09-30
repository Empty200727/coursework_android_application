package ru.kinopolka.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbNetworkTest {

    private val baseUrl = "https://image.tmdb.org/t/p/"

    @Test
    fun `image url uses w342 for lists and w500 for the card`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w342/poster.jpg",
            tmdbImageUrl("/poster.jpg", ImageSize.POSTER_LIST, baseUrl),
        )
        assertEquals(
            "https://image.tmdb.org/t/p/w500/poster.jpg",
            tmdbImageUrl("poster.jpg", ImageSize.POSTER_DETAILS, "https://image.tmdb.org/t/p"),
        )
    }

    @Test
    fun `no image path gives no url`() {
        assertNull(tmdbImageUrl(null, ImageSize.POSTER_LIST, baseUrl))
        assertNull(tmdbImageUrl(" ", ImageSize.POSTER_LIST, baseUrl))
    }

    @Test
    fun `retry delay prefers Retry-After and is capped`() {
        val interceptor = RateLimitRetryInterceptor(baseDelayMillis = 1_000, maxDelayMillis = 10_000)

        assertEquals(3_000L, interceptor.retryDelayMillis("3", attempt = 0))
        assertEquals(1_000L, interceptor.retryDelayMillis(null, attempt = 0))
        assertEquals(4_000L, interceptor.retryDelayMillis("not a number", attempt = 2))
        assertEquals(10_000L, interceptor.retryDelayMillis(null, attempt = 10))
        assertEquals(10_000L, interceptor.retryDelayMillis("120", attempt = 0))
    }
}

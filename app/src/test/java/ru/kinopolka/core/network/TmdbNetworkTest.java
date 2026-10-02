package ru.kinopolka.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TmdbNetworkTest {

    private static final String BASE_URL = "https://image.tmdb.org/t/p/";

    @Test
    public void imageUrlUsesW342ForListsAndW500ForTheCard() {
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg",
                TmdbNetwork.imageUrl("/poster.jpg", ImageSize.POSTER_LIST, BASE_URL));
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg",
                TmdbNetwork.imageUrl("poster.jpg", ImageSize.POSTER_DETAILS, "https://image.tmdb.org/t/p"));
    }

    @Test
    public void noImagePathGivesNoUrl() {
        assertNull(TmdbNetwork.imageUrl(null, ImageSize.POSTER_LIST, BASE_URL));
        assertNull(TmdbNetwork.imageUrl(" ", ImageSize.POSTER_LIST, BASE_URL));
    }

    @Test
    public void retryDelayPrefersRetryAfterAndIsCapped() {
        RateLimitRetryInterceptor interceptor = new RateLimitRetryInterceptor(3, 1_000, 10_000, millis -> { });

        assertEquals(3_000L, interceptor.retryDelayMillis("3", 0));
        assertEquals(1_000L, interceptor.retryDelayMillis(null, 0));
        assertEquals(4_000L, interceptor.retryDelayMillis("not a number", 2));
        assertEquals(10_000L, interceptor.retryDelayMillis(null, 10));
        assertEquals(10_000L, interceptor.retryDelayMillis("120", 0));
    }
}

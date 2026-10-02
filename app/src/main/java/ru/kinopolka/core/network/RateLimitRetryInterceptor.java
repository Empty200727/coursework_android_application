package ru.kinopolka.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.io.InterruptedIOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Repeats a request answered with {@code 429 Too Many Requests} with a growing pause:
 * {@code Retry-After} when the server sends it, otherwise 1 s, 2 s, 4 s… (docs/PLAN.md, section 4).
 * After {@code maxRetries} attempts the last 429 response is returned to the caller.
 */
public final class RateLimitRetryInterceptor implements Interceptor {

    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    public static final String RETRY_AFTER_HEADER = "Retry-After";
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_BASE_DELAY_MILLIS = 1_000L;
    public static final long DEFAULT_MAX_DELAY_MILLIS = 10_000L;
    private static final long MILLIS_IN_SECOND = 1_000L;
    private static final int MAX_SHIFT = 16;

    /** Pause between attempts; replaced in tests. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final int maxRetries;
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final Sleeper sleeper;

    public RateLimitRetryInterceptor() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MILLIS, DEFAULT_MAX_DELAY_MILLIS, Thread::sleep);
    }

    public RateLimitRetryInterceptor(int maxRetries, long baseDelayMillis, long maxDelayMillis, Sleeper sleeper) {
        this.maxRetries = maxRetries;
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.sleeper = sleeper;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        int attempt = 0;
        while (response.code() == HTTP_TOO_MANY_REQUESTS && attempt < maxRetries) {
            long delay = retryDelayMillis(response.header(RETRY_AFTER_HEADER), attempt);
            response.close();
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while waiting to retry");
            }
            attempt++;
            response = chain.proceed(request);
        }
        return response;
    }

    long retryDelayMillis(@Nullable String retryAfter, int attempt) {
        long delay = baseDelayMillis << Math.min(attempt, MAX_SHIFT);
        if (retryAfter != null) {
            try {
                delay = Long.parseLong(retryAfter.trim()) * MILLIS_IN_SECOND;
            } catch (NumberFormatException ignored) {
                // Not a number of seconds (e.g. an HTTP date): the backoff delay is used.
            }
        }
        return Math.max(0, Math.min(delay, maxDelayMillis));
    }
}

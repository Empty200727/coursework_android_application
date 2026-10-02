package ru.kinopolka.core.network;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds the read access token ({@code Authorization: Bearer …}) and {@code language=ru-RU} to
 * every TMDB request. A request that already has a {@code language} parameter keeps it.
 */
public final class TmdbRequestInterceptor implements Interceptor {

    public static final String DEFAULT_LANGUAGE = "ru-RU";
    public static final String LANGUAGE_PARAMETER = "language";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final String token;
    private final String language;

    public TmdbRequestInterceptor(String token) {
        this(token, DEFAULT_LANGUAGE);
    }

    public TmdbRequestInterceptor(String token, String language) {
        this.token = token;
        this.language = language;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        if (url.queryParameter(LANGUAGE_PARAMETER) == null) {
            url = url.newBuilder().addQueryParameter(LANGUAGE_PARAMETER, language).build();
        }
        Request.Builder builder = request.newBuilder().url(url);
        if (!token.isBlank()) {
            builder.header(AUTHORIZATION_HEADER, "Bearer " + token);
        }
        return chain.proceed(builder.build());
    }
}

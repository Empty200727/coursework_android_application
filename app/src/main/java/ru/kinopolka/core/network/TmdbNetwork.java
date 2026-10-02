package ru.kinopolka.core.network;

import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit and image URLs of TMDB. */
public final class TmdbNetwork {

    private TmdbNetwork() {
    }

    /** JSON settings for TMDB: unknown fields are ignored, missing fields become {@code null}. */
    public static Gson gson() {
        return new GsonBuilder().create();
    }

    /** Builds {@link TmdbApi}; used by the Hilt module and by MockWebServer tests. */
    public static TmdbApi createTmdbApi(String baseUrl, OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(TmdbApi.class);
    }

    /** {@code https://image.tmdb.org/t/p/{size}{path}} or {@code null} when TMDB has no image. */
    @Nullable
    public static String imageUrl(@Nullable String path, ImageSize size, String baseUrl) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/" + size.value() + normalizedPath;
    }
}

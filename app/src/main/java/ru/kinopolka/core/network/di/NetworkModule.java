package ru.kinopolka.core.network.di;

import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import ru.kinopolka.BuildConfig;
import ru.kinopolka.core.network.RateLimitRetryInterceptor;
import ru.kinopolka.core.network.TmdbApi;
import ru.kinopolka.core.network.TmdbNetwork;
import ru.kinopolka.core.network.TmdbRequestInterceptor;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    private static final long TIMEOUT_SECONDS = 20L;

    private NetworkModule() {
    }

    @Provides
    @Singleton
    static Gson provideGson() {
        return TmdbNetwork.gson();
    }

    /** Base client without the TMDB token: it is also used to download posters. */
    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    static TmdbApi provideTmdbApi(OkHttpClient client, Gson gson) {
        OkHttpClient.Builder builder = client.newBuilder()
                .addInterceptor(new TmdbRequestInterceptor(BuildConfig.TMDB_TOKEN))
                .addInterceptor(new RateLimitRetryInterceptor());
        if (BuildConfig.DEBUG) {
            // BASIC logs method, URL and status only: the token header is never printed.
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC));
        }
        return TmdbNetwork.createTmdbApi(BuildConfig.TMDB_API_BASE_URL, builder.build(), gson);
    }
}

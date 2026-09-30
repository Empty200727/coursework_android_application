package ru.kinopolka.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ru.kinopolka.BuildConfig
import ru.kinopolka.core.network.RateLimitRetryInterceptor
import ru.kinopolka.core.network.TmdbApi
import ru.kinopolka.core.network.TmdbJson
import ru.kinopolka.core.network.TmdbRequestInterceptor
import ru.kinopolka.core.network.createTmdbApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 20L

    @Provides
    @Singleton
    fun provideJson(): Json = TmdbJson

    /** Base client without the TMDB token: it is shared with the image loader. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi {
        val tmdbClient = client.newBuilder()
            .addInterceptor(TmdbRequestInterceptor(BuildConfig.TMDB_TOKEN))
            .addInterceptor(RateLimitRetryInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC logs method, URL and status only: the token header is never printed.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()
        return createTmdbApi(BuildConfig.TMDB_API_BASE_URL, tmdbClient, json)
    }
}

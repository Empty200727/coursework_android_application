package ru.kinopolka.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** JSON settings for TMDB: unknown fields are ignored, `null` falls back to defaults. */
val TmdbJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/** Builds [TmdbApi]; used by the Hilt module and by MockWebServer tests. */
fun createTmdbApi(baseUrl: String, client: OkHttpClient, json: Json = TmdbJson): TmdbApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create()

/** TMDB image sizes (N-04): `w342` for lists, `w500` for the title card. */
enum class ImageSize(val value: String) {
    POSTER_LIST("w342"),
    POSTER_DETAILS("w500"),
    BACKDROP("w780"),
    PROFILE("w185"),
}

/** `https://image.tmdb.org/t/p/{size}{path}` or `null` when TMDB has no image. */
fun tmdbImageUrl(path: String?, size: ImageSize, baseUrl: String): String? {
    if (path.isNullOrBlank()) return null
    val normalizedPath = if (path.startsWith('/')) path else "/$path"
    return baseUrl.trimEnd('/') + "/" + size.value + normalizedPath
}

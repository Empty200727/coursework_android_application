package ru.kinopolka.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the read access token (`Authorization: Bearer …`) and `language=ru-RU` to every
 * TMDB request. A request that already has a `language` parameter keeps it.
 */
class TmdbRequestInterceptor(private val token: String, private val language: String = DEFAULT_LANGUAGE) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = if (request.url.queryParameter(LANGUAGE_PARAMETER) == null) {
            request.url.newBuilder().addQueryParameter(LANGUAGE_PARAMETER, language).build()
        } else {
            request.url
        }
        val builder = request.newBuilder().url(url)
        if (token.isNotBlank()) {
            builder.header(AUTHORIZATION_HEADER, "Bearer $token")
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru-RU"
        const val LANGUAGE_PARAMETER = "language"
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}

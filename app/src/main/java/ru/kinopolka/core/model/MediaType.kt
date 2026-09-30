package ru.kinopolka.core.model

/**
 * Type of a TMDB title. TMDB ids are unique only within a type, so a title is always
 * identified by the pair (type, id) — see [MediaKey].
 *
 * [key] is the value TMDB uses in URLs and in the `media_type` field.
 */
enum class MediaType(val key: String) {
    MOVIE("movie"),
    TV("tv"),
    ;

    companion object {
        fun fromKey(key: String?): MediaType? = entries.firstOrNull { it.key == key }
    }
}

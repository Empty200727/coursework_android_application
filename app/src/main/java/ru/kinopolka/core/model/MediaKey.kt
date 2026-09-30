package ru.kinopolka.core.model

/** Unique key of a title: TMDB ids of movies and series may coincide. */
data class MediaKey(val mediaType: MediaType, val tmdbId: Int)

package ru.kinopolka.core.model;

/** Unique key of a title: TMDB ids of movies and series may coincide. */
public record MediaKey(MediaType mediaType, int tmdbId) {
}

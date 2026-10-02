package ru.kinopolka.core.model;

/** A genre exactly as TMDB returns it: ids differ between movies and series. */
public record TmdbGenre(MediaType mediaType, int id, String name) {
}

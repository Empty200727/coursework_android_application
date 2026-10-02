package ru.kinopolka.core.model;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

/**
 * A movie or a series as shown in lists, shelves and search results.
 *
 * @param releaseDate release date (first air date for series), {@code null} when unknown
 */
public record Media(
        MediaKey key,
        String title,
        @Nullable String originalTitle,
        @Nullable String overview,
        @Nullable String posterPath,
        @Nullable String backdropPath,
        @Nullable LocalDate releaseDate,
        double voteAverage,
        int voteCount,
        double popularity,
        List<Integer> genreIds) {

    public Media {
        genreIds = List.copyOf(genreIds);
    }

    public MediaType mediaType() {
        return key.mediaType();
    }

    public int tmdbId() {
        return key.tmdbId();
    }

    /** Year of release, {@code null} when TMDB has no date. */
    @Nullable
    public Integer releaseYear() {
        return releaseDate == null ? null : releaseDate.getYear();
    }

    public Media withTitle(String newTitle) {
        return new Media(key, newTitle, originalTitle, overview, posterPath, backdropPath, releaseDate,
                voteAverage, voteCount, popularity, genreIds);
    }

    public Media withOverview(@Nullable String newOverview) {
        return new Media(key, title, originalTitle, newOverview, posterPath, backdropPath, releaseDate,
                voteAverage, voteCount, popularity, genreIds);
    }

    public Media withGenreIds(List<Integer> newGenreIds) {
        return new Media(key, title, originalTitle, overview, posterPath, backdropPath, releaseDate,
                voteAverage, voteCount, popularity, newGenreIds);
    }
}

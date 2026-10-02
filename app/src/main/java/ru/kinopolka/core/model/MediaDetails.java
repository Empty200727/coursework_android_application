package ru.kinopolka.core.model;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

/**
 * Full information for the title card (F-08…F-10).
 *
 * @param runtimeMinutes movie runtime, {@code null} for series
 * @param numberOfSeasons number of seasons, {@code null} for movies
 * @param lastAirDate last air date of a series, {@code null} for movies
 * @param inProduction whether a series is still running, {@code null} for movies
 * @param overviewFallback the Russian overview was missing and the English one is shown (N-07)
 * @param complete {@code false} while only list data is cached: the card has not been loaded yet
 */
public record MediaDetails(
        Media media,
        List<TmdbGenre> genres,
        @Nullable Integer runtimeMinutes,
        @Nullable Integer numberOfSeasons,
        @Nullable LocalDate lastAirDate,
        @Nullable Boolean inProduction,
        boolean overviewFallback,
        List<CastMember> cast,
        List<Media> recommendations,
        List<Media> similar,
        boolean complete) {

    public MediaDetails {
        genres = List.copyOf(genres);
        cast = List.copyOf(cast);
        recommendations = List.copyOf(recommendations);
        similar = List.copyOf(similar);
    }

    /** F-10: recommendations first; if TMDB has none, similar titles. */
    public List<Media> related() {
        return recommendations.isEmpty() ? similar : recommendations;
    }

    public MediaDetails withEnglishOverview(String overview) {
        return new MediaDetails(media.withOverview(overview), genres, runtimeMinutes, numberOfSeasons, lastAirDate,
                inProduction, true, cast, recommendations, similar, complete);
    }

    public MediaDetails withRecommendations(List<Media> newRecommendations) {
        return new MediaDetails(media, genres, runtimeMinutes, numberOfSeasons, lastAirDate, inProduction,
                overviewFallback, cast, newRecommendations, similar, complete);
    }
}

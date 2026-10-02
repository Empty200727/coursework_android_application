package ru.kinopolka.core.data;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;

/**
 * Unified genre catalog (F-04, docs/PLAN.md, section 4).
 *
 * <p>TMDB uses different ids and partly different genres for movies and series. The static
 * table below links them: for the «Все» filter the user sees one genre while the app sends
 * the right id for each type. Names are taken from TMDB answers in Russian; {@code fallbackName}
 * is used only if TMDB does not return a name.
 */
public final class GenreCatalog {

    public record Mapping(String key, @Nullable Integer movieGenreId, @Nullable Integer tvGenreId,
            String fallbackName) {
    }

    public static final List<Mapping> MAPPINGS = List.of(
            new Mapping("action", 28, 10759, "Боевик"),
            new Mapping("comedy", 35, 35, "Комедия"),
            new Mapping("drama", 18, 18, "Драма"),
            new Mapping("animation", 16, 16, "Мультфильм"),
            new Mapping("fantasy", 14, 10765, "Фэнтези"),
            new Mapping("science_fiction", 878, 10765, "Фантастика"),
            new Mapping("crime", 80, 80, "Криминал"),
            new Mapping("mystery", 9648, 9648, "Детектив"),
            new Mapping("thriller", 53, null, "Триллер"),
            new Mapping("horror", 27, null, "Ужасы"),
            new Mapping("adventure", 12, 10759, "Приключения"),
            new Mapping("family", 10751, 10751, "Семейный"),
            new Mapping("romance", 10749, null, "Мелодрама"),
            new Mapping("documentary", 99, 99, "Документальный"),
            new Mapping("war", 10752, 10768, "Военный"),
            new Mapping("history", 36, null, "История"),
            new Mapping("western", 37, 37, "Вестерн"),
            new Mapping("music", 10402, null, "Музыка"),
            new Mapping("tv_movie", 10770, null, "Телевизионный фильм"),
            new Mapping("kids", null, 10762, "Детский"),
            new Mapping("reality", null, 10764, "Реалити-шоу"),
            new Mapping("soap", null, 10766, "Мыльная опера"),
            new Mapping("talk", null, 10767, "Ток-шоу"),
            new Mapping("news", null, 10763, "Новости"));

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");

    private GenreCatalog() {
    }

    /**
     * Builds the unified catalog from the TMDB genre lists of both types.
     *
     * <ul>
     *   <li>A mapped id that TMDB no longer returns is dropped; a mapping with no ids left is dropped.
     *   <li>A TMDB genre missing from the table becomes a separate one-type genre ({@code movie-<id>},
     *       {@code tv-<id>}), so a new TMDB genre is never lost.
     *   <li>The name of the movie genre wins: «Боевик» rather than «Боевик и Приключения».
     * </ul>
     */
    public static List<Genre> merge(List<TmdbGenre> genres) {
        // MediaKey is reused here as a (type, genre id) pair.
        Map<MediaKey, String> names = new HashMap<>();
        for (TmdbGenre genre : genres) {
            names.put(new MediaKey(genre.mediaType(), genre.id()), genre.name());
        }

        List<Genre> result = new ArrayList<>();
        Set<MediaKey> mappedIds = new HashSet<>();
        for (Mapping mapping : MAPPINGS) {
            if (mapping.movieGenreId() != null) {
                mappedIds.add(new MediaKey(MediaType.MOVIE, mapping.movieGenreId()));
            }
            if (mapping.tvGenreId() != null) {
                mappedIds.add(new MediaKey(MediaType.TV, mapping.tvGenreId()));
            }
            Integer movieId = known(names, MediaType.MOVIE, mapping.movieGenreId());
            Integer tvId = known(names, MediaType.TV, mapping.tvGenreId());
            if (movieId == null && tvId == null) {
                continue;
            }
            String name = movieId != null
                    ? names.get(new MediaKey(MediaType.MOVIE, movieId))
                    : names.get(new MediaKey(MediaType.TV, tvId));
            if (name == null) {
                name = mapping.fallbackName();
            }
            result.add(new Genre(mapping.key(), displayName(name), movieId, tvId));
        }

        Set<MediaKey> added = new HashSet<>();
        for (TmdbGenre genre : genres) {
            MediaKey id = new MediaKey(genre.mediaType(), genre.id());
            if (mappedIds.contains(id) || !added.add(id)) {
                continue;
            }
            boolean movie = genre.mediaType() == MediaType.MOVIE;
            result.add(new Genre(
                    genre.mediaType().key() + "-" + genre.id(),
                    displayName(genre.name()),
                    movie ? genre.id() : null,
                    movie ? null : genre.id()));
        }
        return result;
    }

    @Nullable
    private static Integer known(Map<MediaKey, String> names, MediaType type, @Nullable Integer id) {
        return id != null && names.containsKey(new MediaKey(type, id)) ? id : null;
    }

    /** TMDB returns Russian genre names in lower case («боевик»): «Боевик». */
    public static String displayName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        int first = trimmed.codePointAt(0);
        int length = Character.charCount(first);
        return trimmed.substring(0, length).toUpperCase(RUSSIAN) + trimmed.substring(length);
    }
}

package ru.kinopolka.feature.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;

/** Shelves of the home screen (F-05, F-07). */
public final class HomeShelves {

    /** Genres of the home shelves in display order; genres missing for the filter are skipped. */
    static final List<String> HOME_GENRE_KEYS = List.of(
            "action",
            "comedy",
            "drama",
            "animation",
            "science_fiction",
            "crime",
            "horror",
            "family",
            "mystery",
            "documentary");

    /** F-05: 6–8 genre shelves. */
    static final int MAX_GENRE_SHELVES = 8;

    private HomeShelves() {
    }

    /** Trending, popular movies and/or series, then genre shelves available for the filter. */
    public static List<Shelf> homeShelves(MediaFilter filter, List<Genre> genres) {
        List<Shelf> shelves = new ArrayList<>();
        shelves.add(new Shelf.Trending(filter));
        if (filter.includes(MediaType.MOVIE)) {
            shelves.add(new Shelf.Popular(MediaType.MOVIE));
        }
        if (filter.includes(MediaType.TV)) {
            shelves.add(new Shelf.Popular(MediaType.TV));
        }
        Map<String, Genre> byKey = new HashMap<>();
        for (Genre genre : genres) {
            byKey.put(genre.key(), genre);
        }
        int genreShelves = 0;
        for (String key : HOME_GENRE_KEYS) {
            Genre genre = byKey.get(key);
            if (genre != null && genre.matches(filter) && genreShelves < MAX_GENRE_SHELVES) {
                shelves.add(new Shelf.ByGenre(genre, filter));
                genreShelves++;
            }
        }
        return shelves;
    }

    /** A shelf list built for the previous filter may still be on screen for one frame. */
    static boolean matches(Shelf shelf, MediaFilter filter) {
        if (shelf instanceof Shelf.Trending trending) {
            return trending.filter() == filter;
        }
        if (shelf instanceof Shelf.Popular popular) {
            return filter.includes(popular.mediaType());
        }
        return ((Shelf.ByGenre) shelf).filter() == filter;
    }
}

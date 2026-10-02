package ru.kinopolka.core.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.network.DiscoverSort;
import ru.kinopolka.core.network.TmdbApi;

/** Merging of movie and series lists for the «Все» filter. */
public final class MediaMerge {

    private MediaMerge() {
    }

    public static List<Media> mergeMedia(List<List<Media>> lists) {
        return mergeMedia(lists, MediaSort.POPULARITY, TmdbApi.PAGE_SIZE);
    }

    /** One list ordered by {@code sort}, without duplicates, at most {@code limit} items. */
    public static List<Media> mergeMedia(List<List<Media>> lists, MediaSort sort, int limit) {
        Map<MediaKey, Media> unique = new LinkedHashMap<>();
        for (List<Media> list : lists) {
            for (Media media : list) {
                unique.putIfAbsent(media.key(), media);
            }
        }
        List<Media> result = new ArrayList<>(unique.values());
        result.sort(comparator(sort));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    /** Descending order used by TMDB discover for the same sort. */
    public static Comparator<Media> comparator(MediaSort sort) {
        return switch (sort) {
            case POPULARITY -> Comparator.comparingDouble(Media::popularity).reversed();
            case RATING -> Comparator.comparingDouble(Media::voteAverage).reversed()
                    .thenComparing(Comparator.comparingInt(Media::voteCount).reversed());
            // Unknown dates go last.
            case NEWEST -> Comparator.comparing(Media::releaseDate,
                    Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()));
        };
    }

    public static DiscoverSort mapMediaSort(MediaSort sort) {
        return switch (sort) {
            case POPULARITY -> DiscoverSort.POPULARITY;
            case RATING -> DiscoverSort.RATING;
            case NEWEST -> DiscoverSort.NEWEST;
        };
    }
}

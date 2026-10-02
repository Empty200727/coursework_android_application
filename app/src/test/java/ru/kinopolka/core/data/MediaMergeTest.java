package ru.kinopolka.core.data;

import static org.junit.Assert.assertEquals;
import static ru.kinopolka.testing.TestData.testMedia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.network.TmdbApi;

/** «Все»: movies and series of a shelf are merged into one list (docs/PLAN.md, section 4). */
public class MediaMergeTest {

    private final List<Media> movies = List.of(
            testMedia(1, MediaType.MOVIE, 90.0),
            testMedia(2, MediaType.MOVIE, 50.0),
            testMedia(3, MediaType.MOVIE, 10.0));
    private final List<Media> series = List.of(
            testMedia(1, MediaType.TV, 70.0),
            testMedia(4, MediaType.TV, 20.0));

    private static List<MediaKey> keys(List<Media> media) {
        return media.stream().map(Media::key).toList();
    }

    @Test
    public void moviesAndSeriesAreMergedByPopularity() {
        List<Media> merged = MediaMerge.mergeMedia(List.of(movies, series));

        assertEquals(List.of(
                new MediaKey(MediaType.MOVIE, 1),
                new MediaKey(MediaType.TV, 1),
                new MediaKey(MediaType.MOVIE, 2),
                new MediaKey(MediaType.TV, 4),
                new MediaKey(MediaType.MOVIE, 3)), keys(merged));
    }

    @Test
    public void mergedShelfKeeps20Items() {
        List<Media> manyMovies = new ArrayList<>();
        List<Media> manySeries = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            manyMovies.add(testMedia(i, MediaType.MOVIE, i));
            manySeries.add(testMedia(i, MediaType.TV, i + 0.5));
        }

        List<Media> merged = MediaMerge.mergeMedia(List.of(manyMovies, manySeries));

        assertEquals(TmdbApi.PAGE_SIZE, merged.size());
        assertEquals(new MediaKey(MediaType.TV, 20), merged.get(0).key());
        assertEquals(11.0, merged.get(merged.size() - 1).popularity(), 0.0);
    }

    @Test
    public void duplicatesAndEmptyListsAreHandled() {
        assertEquals(keys(movies), keys(MediaMerge.mergeMedia(List.of(movies, List.of(), movies))));
        assertEquals(List.of(), MediaMerge.mergeMedia(List.of()));
    }

    @Test
    public void otherSortOrdersAreSupported() {
        List<Media> rated = List.of(
                testMedia(1, MediaType.MOVIE, 1.0, 6.0, LocalDate.of(2024, 1, 1)),
                testMedia(2, MediaType.MOVIE, 1.0, 9.0, null),
                testMedia(3, MediaType.TV, 1.0, 8.0, LocalDate.of(2025, 5, 1)));

        assertEquals(List.of(2, 3, 1), MediaMerge.mergeMedia(List.of(rated), MediaSort.RATING, 20).stream()
                .map(Media::tmdbId).toList());
        assertEquals(List.of(3, 1, 2), MediaMerge.mergeMedia(List.of(rated), MediaSort.NEWEST, 20).stream()
                .map(Media::tmdbId).toList());
    }
}

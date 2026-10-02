package ru.kinopolka.testing;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.PagedResponseDto;

/** Builders of test data. */
public final class TestData {

    public static final Genre ACTION = new Genre("action", "Боевик", 28, 10759);
    public static final Genre HORROR = new Genre("horror", "Ужасы", 27, null);
    public static final Genre DRAMA = new Genre("drama", "Драма", 18, 18);

    private TestData() {
    }

    public static MediaListItemDto movieDto(int id) {
        return movieDto(id, 1.0, 7.0, "2020-01-01", null);
    }

    public static MediaListItemDto movieDto(int id, double popularity, double voteAverage, String releaseDate,
            @Nullable String mediaType) {
        return new MediaListItemDto(id, mediaType, "Фильм " + id, null, null, null, null, null, null, releaseDate,
                null, voteAverage, 500, popularity, List.of(28), null);
    }

    public static MediaListItemDto tvDto(int id) {
        return tvDto(id, 1.0, 7.0, "2020-01-01", null);
    }

    public static MediaListItemDto tvDto(int id, double popularity, double voteAverage, String firstAirDate,
            @Nullable String mediaType) {
        return new MediaListItemDto(id, mediaType, null, "Сериал " + id, null, null, null, null, null, null,
                firstAirDate, voteAverage, 500, popularity, List.of(18), null);
    }

    public static PagedResponseDto<MediaListItemDto> pageOf(List<MediaListItemDto> items) {
        return pageOf(items, 1, 1);
    }

    public static PagedResponseDto<MediaListItemDto> pageOf(List<MediaListItemDto> items, int page, int totalPages) {
        return new PagedResponseDto<>(page, items, totalPages, items.size());
    }

    public static Media testMedia(int id) {
        return testMedia(id, MediaType.MOVIE);
    }

    public static Media testMedia(int id, MediaType type) {
        return testMedia(id, type, 1.0, 7.0, LocalDate.of(2020, 1, 1));
    }

    public static Media testMedia(int id, MediaType type, double popularity) {
        return testMedia(id, type, popularity, 7.0, LocalDate.of(2020, 1, 1));
    }

    public static Media testMedia(int id, MediaType type, double popularity, double voteAverage,
            @Nullable LocalDate releaseDate) {
        return new Media(new MediaKey(type, id), "Произведение " + id, null, null, null, null, releaseDate,
                voteAverage, 500, popularity, List.of());
    }
}

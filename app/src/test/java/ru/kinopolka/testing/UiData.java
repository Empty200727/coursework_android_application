package ru.kinopolka.testing;

import java.time.LocalDate;
import java.util.List;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;

/** Titles shown in UI tests. */
public final class UiData {

    public static final Media MATRIX = new Media(new MediaKey(MediaType.MOVIE, 603), "Матрица", "The Matrix",
            "Жизнь Томаса Андерсона разделена на две части.", null, null, LocalDate.of(1999, 3, 31), 8.2, 25643, 85.3,
            List.of(28));

    public static final Media ARCANE = new Media(new MediaKey(MediaType.TV, 94605), "Аркейн", "Arcane", null, null,
            null, LocalDate.of(2021, 11, 6), 8.7, 4200, 120.0, List.of(16));

    public static final MediaDetails MATRIX_DETAILS = new MediaDetails(MATRIX,
            List.of(new TmdbGenre(MediaType.MOVIE, 28, "Боевик")), 136, null, null, null, false,
            List.of(new CastMember(6384, "Киану Ривз", "Нео", null, 0)), List.of(ARCANE), List.of(), true);

    private UiData() {
    }
}

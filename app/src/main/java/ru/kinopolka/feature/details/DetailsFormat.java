package ru.kinopolka.feature.details;

import androidx.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Locale;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaType;

/** Texts of the title card. */
final class DetailsFormat {

    static final int MINUTES_IN_HOUR = 60;

    private DetailsFormat() {
    }

    /** «1999» for a movie; «2008–2013» or «2019–н. в.» for a series ({@code ongoing} is the «н. в.» text). */
    @Nullable
    static String yearsText(MediaDetails details, String ongoing) {
        Integer start = details.media().releaseYear();
        if (start == null) {
            return null;
        }
        if (details.media().mediaType() == MediaType.MOVIE) {
            return start.toString();
        }
        String end = null;
        if (Boolean.TRUE.equals(details.inProduction())) {
            end = ongoing;
        } else if (details.lastAirDate() != null && details.lastAirDate().getYear() != start) {
            end = String.valueOf(details.lastAirDate().getYear());
        }
        return end == null ? start.toString() : start + "–" + end;
    }

    /** «29 870» — grouped the Russian way. */
    static String formatCount(int value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(value);
    }
}

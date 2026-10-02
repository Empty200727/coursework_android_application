package ru.kinopolka.core.ui;

import android.content.Context;
import androidx.annotation.StringRes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaType;

/** Texts shared by several screens. */
public final class Formats {

    private Formats() {
    }

    /** «8.9» with a dot, as TMDB shows ratings. */
    public static String rating(double voteAverage) {
        return String.format(Locale.ROOT, "%.1f", voteAverage);
    }

    /** «2008 · ★ 8.9»: year and rating when TMDB has them. */
    public static String subtitle(Context context, Media media) {
        List<String> parts = new ArrayList<>();
        if (media.releaseYear() != null) {
            parts.add(String.valueOf(media.releaseYear()));
        }
        if (media.voteCount() > 0) {
            parts.add(context.getString(R.string.rating_short, rating(media.voteAverage())));
        }
        return String.join(" · ", parts);
    }

    public static String typeLabel(Context context, MediaType type) {
        return context.getString(type == MediaType.MOVIE ? R.string.media_type_movie : R.string.media_type_tv);
    }

    @StringRes
    public static int filterLabel(MediaFilter filter) {
        return switch (filter) {
            case ALL -> R.string.filter_all;
            case MOVIES -> R.string.filter_movies;
            case SERIES -> R.string.filter_series;
        };
    }

    @StringRes
    public static int errorMessage(DataError error) {
        return switch (error) {
            case NO_CONNECTION -> R.string.error_no_connection;
            case UNAUTHORIZED -> R.string.error_unauthorized;
            case NOT_FOUND, SERVER, BAD_RESPONSE -> R.string.error_generic;
        };
    }
}

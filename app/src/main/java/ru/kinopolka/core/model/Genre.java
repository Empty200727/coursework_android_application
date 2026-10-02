package ru.kinopolka.core.model;

import androidx.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/**
 * A genre of the unified catalog (F-04). The user sees one genre while the app substitutes
 * the matching TMDB id for each media type; a missing id means the genre does not exist
 * for that type (e.g. «Ужасы» has no series counterpart).
 */
public record Genre(String key, String name, @Nullable Integer movieGenreId, @Nullable Integer tvGenreId) {

    @Nullable
    public Integer idFor(MediaType type) {
        return type == MediaType.MOVIE ? movieGenreId : tvGenreId;
    }

    /** Media types for which this genre can be requested. */
    public Set<MediaType> mediaTypes() {
        Set<MediaType> types = EnumSet.noneOf(MediaType.class);
        for (MediaType type : MediaType.values()) {
            if (idFor(type) != null) {
                types.add(type);
            }
        }
        return types;
    }

    /** Whether the genre has content for at least one type allowed by the filter. */
    public boolean matches(MediaFilter filter) {
        for (MediaType type : filter.mediaTypes()) {
            if (idFor(type) != null) {
                return true;
            }
        }
        return false;
    }
}

package ru.kinopolka.core.model;

import androidx.annotation.Nullable;

/**
 * Type of a TMDB title. TMDB ids are unique only within a type, so a title is always
 * identified by the pair (type, id) — see {@link MediaKey}.
 *
 * <p>{@link #key()} is the value TMDB uses in URLs and in the {@code media_type} field.
 */
public enum MediaType {
    MOVIE("movie"),
    TV("tv");

    private final String key;

    MediaType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    @Nullable
    public static MediaType fromKey(@Nullable String key) {
        for (MediaType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }
}

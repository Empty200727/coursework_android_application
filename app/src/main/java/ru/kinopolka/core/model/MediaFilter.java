package ru.kinopolka.core.model;

import java.util.EnumSet;
import java.util.Set;

/** The «Все / Фильмы / Сериалы» filter (F-02). */
public enum MediaFilter {
    ALL(EnumSet.of(MediaType.MOVIE, MediaType.TV)),
    MOVIES(EnumSet.of(MediaType.MOVIE)),
    SERIES(EnumSet.of(MediaType.TV));

    private final Set<MediaType> mediaTypes;

    MediaFilter(Set<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    /** Types in declaration order: movies before series. */
    public Set<MediaType> mediaTypes() {
        return EnumSet.copyOf(mediaTypes);
    }

    public boolean includes(MediaType type) {
        return mediaTypes.contains(type);
    }

    /** Parses a saved name; unknown values fall back to {@link #ALL}. */
    public static MediaFilter fromName(String name) {
        for (MediaFilter filter : values()) {
            if (filter.name().equals(name)) {
                return filter;
            }
        }
        return ALL;
    }
}

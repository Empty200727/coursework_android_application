package ru.kinopolka.core.model;

import androidx.annotation.Nullable;

/** Actor in the title card (F-09). */
public record CastMember(
        int personId, String name, @Nullable String character, @Nullable String profilePath, int order) {
}

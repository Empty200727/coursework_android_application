package ru.kinopolka.feature.genre;

import androidx.annotation.Nullable;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;

/** @param title {@code null} until the genre is found in the catalog */
public record GenreUiState(@Nullable String title, MediaFilter filter, MediaSort sort, boolean offline) {
}

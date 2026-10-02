package ru.kinopolka.core.model;

import androidx.annotation.Nullable;
import java.time.Instant;

/** A title in the user's library («Моя полка»). */
public record LibraryEntry(
        MediaKey key,
        WatchStatus watchStatus,
        boolean favorite,
        Instant addedAt,
        @Nullable Instant watchedAt,
        @Nullable Integer userRating,
        @Nullable String localPosterPath) {

    /** An entry with no status and no favorite flag must be removed from the database. */
    public boolean isEmpty() {
        return watchStatus == WatchStatus.NONE && !favorite;
    }

    public LibraryEntry withStatus(WatchStatus status, Instant newAddedAt, @Nullable Instant newWatchedAt) {
        return new LibraryEntry(key, status, favorite, newAddedAt, newWatchedAt, userRating, localPosterPath);
    }

    public LibraryEntry withFavorite(boolean isFavorite) {
        return new LibraryEntry(key, watchStatus, isFavorite, addedAt, watchedAt, userRating, localPosterPath);
    }

    public LibraryEntry withPoster(@Nullable String path) {
        return new LibraryEntry(key, watchStatus, favorite, addedAt, watchedAt, userRating, path);
    }
}

package ru.kinopolka.core.model;

/** Tabs of «Моя полка» (F-12). */
public enum LibraryTab {
    WANT,
    WATCHED,
    FAVORITES;

    public boolean contains(LibraryEntry entry) {
        return switch (this) {
            case WANT -> entry.watchStatus() == WatchStatus.WANT;
            case WATCHED -> entry.watchStatus() == WatchStatus.WATCHED;
            case FAVORITES -> entry.favorite();
        };
    }
}

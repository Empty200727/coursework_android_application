package ru.kinopolka.core.data.library;

import androidx.annotation.Nullable;
import java.text.Collator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.WatchStatus;

/**
 * Status rule (docs/PLAN.md, section 2): «Хочу посмотреть» and «Смотрел» are one status field,
 * so they exclude each other; «Избранное» is an independent flag. An entry with no status and
 * no flag is removed.
 */
public final class LibraryRules {

    private static final Collator RUSSIAN_COLLATOR = Collator.getInstance(Locale.forLanguageTag("ru"));

    private LibraryRules() {
    }

    private static LibraryEntry newEntry(MediaKey key, Instant now) {
        return new LibraryEntry(key, WatchStatus.NONE, false, now, null, null, null);
    }

    /** Sets {@code status}; «Смотрел» records the date, a new status moves the title to the top of its tab. */
    public static LibraryEntry withWatchStatus(@Nullable LibraryEntry current, MediaKey key, WatchStatus status,
            Instant now) {
        LibraryEntry entry = current != null ? current : newEntry(key, now);
        if (entry.watchStatus() == status) {
            return entry;
        }
        return entry.withStatus(
                status,
                status == WatchStatus.NONE ? entry.addedAt() : now,
                status == WatchStatus.WATCHED ? now : null);
    }

    public static LibraryEntry withFavorite(@Nullable LibraryEntry current, MediaKey key, boolean favorite,
            Instant now) {
        LibraryEntry entry = current != null ? current : newEntry(key, now);
        return entry.withFavorite(favorite);
    }

    /** F-13: removal from a tab clears only what this tab shows. */
    public static LibraryEntry removedFrom(LibraryEntry entry, LibraryTab tab) {
        return switch (tab) {
            case WANT, WATCHED -> entry.withStatus(WatchStatus.NONE, entry.addedAt(), null);
            case FAVORITES -> entry.withFavorite(false);
        };
    }

    /** Items of {@code tab} filtered by type and sorted (F-12). */
    public static List<LibraryItem> forTab(List<LibraryItem> all, LibraryTab tab, MediaFilter filter,
            LibrarySort sort) {
        List<LibraryItem> items = new ArrayList<>();
        for (LibraryItem item : all) {
            if (tab.contains(item.entry()) && filter.includes(item.media().mediaType())) {
                items.add(item);
            }
        }
        Comparator<LibraryItem> comparator = switch (sort) {
            case ADDED -> Comparator.comparing((LibraryItem item) -> item.entry().addedAt()).reversed();
            case TITLE -> Comparator.comparing((LibraryItem item) -> item.media().title(), RUSSIAN_COLLATOR);
            case RATING -> Comparator.comparingDouble((LibraryItem item) -> item.media().voteAverage()).reversed()
                    .thenComparing(Comparator.comparingInt((LibraryItem item) -> item.media().voteCount()).reversed());
        };
        items.sort(comparator);
        return items;
    }

    /** Counters of the tabs for the selected type filter. */
    public static Map<LibraryTab, Integer> tabCounts(List<LibraryItem> all, MediaFilter filter) {
        Map<LibraryTab, Integer> counts = new EnumMap<>(LibraryTab.class);
        for (LibraryTab tab : LibraryTab.values()) {
            int count = 0;
            for (LibraryItem item : all) {
                if (tab.contains(item.entry()) && filter.includes(item.media().mediaType())) {
                    count++;
                }
            }
            counts.put(tab, count);
        }
        return counts;
    }
}

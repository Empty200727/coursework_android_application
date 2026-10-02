package ru.kinopolka.core.ui;

import java.util.List;
import java.util.function.Consumer;
import ru.kinopolka.R;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.databinding.ViewFilterChipsBinding;

/** The «Все / Фильмы / Сериалы» chips (F-02). */
public final class FilterChips {

    private FilterChips() {
    }

    public static void setup(ViewFilterChipsBinding binding, Consumer<MediaFilter> onSelect) {
        binding.filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            MediaFilter filter = fromIds(checkedIds);
            if (filter != null) {
                onSelect.accept(filter);
            }
        });
    }

    /** Checks the chip of {@code filter}; does nothing when it is already checked. */
    public static void select(ViewFilterChipsBinding binding, MediaFilter filter) {
        int id = idOf(filter);
        if (binding.filterGroup.getCheckedChipId() != id) {
            binding.filterGroup.check(id);
        }
    }

    private static int idOf(MediaFilter filter) {
        return switch (filter) {
            case ALL -> R.id.filter_all;
            case MOVIES -> R.id.filter_movies;
            case SERIES -> R.id.filter_series;
        };
    }

    private static MediaFilter fromIds(List<Integer> ids) {
        for (MediaFilter filter : MediaFilter.values()) {
            if (ids.contains(idOf(filter))) {
                return filter;
            }
        }
        return null;
    }
}

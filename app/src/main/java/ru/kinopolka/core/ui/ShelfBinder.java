package ru.kinopolka.core.ui;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import java.util.function.Consumer;
import ru.kinopolka.R;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.databinding.ItemShelfBinding;

/**
 * Horizontal shelf (F-05, F-07): title, optional «Все» button and up to 20 posters.
 * Shows skeletons while loading and nothing is cached yet.
 */
public final class ShelfBinder {

    private final ItemShelfBinding binding;
    private final PosterAdapter adapter;

    public ShelfBinder(ItemShelfBinding binding, Consumer<MediaKey> onOpenMedia) {
        this.binding = binding;
        this.adapter = new PosterAdapter(onOpenMedia);
        binding.shelfItems.setLayoutManager(
                new LinearLayoutManager(binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.shelfItems.setAdapter(adapter);
    }

    public void bind(String title, List<Media> items, boolean loading, @Nullable Runnable onSeeAll) {
        binding.shelfTitle.setText(title);
        if (onSeeAll != null) {
            binding.shelfSeeAll.setVisibility(View.VISIBLE);
            binding.shelfSeeAll.setContentDescription(
                    binding.getRoot().getContext().getString(R.string.shelf_see_all_description, title));
            binding.shelfSeeAll.setOnClickListener(v -> onSeeAll.run());
        } else {
            binding.shelfSeeAll.setVisibility(View.GONE);
            binding.shelfSeeAll.setOnClickListener(null);
        }
        boolean skeleton = items.isEmpty() && loading;
        binding.shelfSkeleton.getRoot().setVisibility(skeleton ? View.VISIBLE : View.GONE);
        binding.shelfItems.setVisibility(skeleton ? View.GONE : View.VISIBLE);
        adapter.submitList(items);
    }
}

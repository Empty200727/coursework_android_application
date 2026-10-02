package ru.kinopolka.feature.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import ru.kinopolka.R;
import ru.kinopolka.core.model.LibraryItem;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.network.ImageSize;
import ru.kinopolka.core.ui.Formats;
import ru.kinopolka.core.ui.Images;
import ru.kinopolka.databinding.ItemMediaRowBinding;

/** Rows of «Моя полка»: poster (saved file offline), title, type, year, rating and «Удалить». */
final class LibraryAdapter extends ListAdapter<LibraryItem, LibraryAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<LibraryItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull LibraryItem oldItem, @NonNull LibraryItem newItem) {
            return oldItem.media().key().equals(newItem.media().key());
        }

        @Override
        public boolean areContentsTheSame(@NonNull LibraryItem oldItem, @NonNull LibraryItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final Consumer<MediaKey> onOpenMedia;
    private final Consumer<LibraryItem> onRemove;
    private LibraryTab tab = LibraryTab.WANT;

    LibraryAdapter(Consumer<MediaKey> onOpenMedia, Consumer<LibraryItem> onRemove) {
        super(DIFF);
        this.onOpenMedia = onOpenMedia;
        this.onRemove = onRemove;
    }

    void submit(List<LibraryItem> items, LibraryTab newTab) {
        boolean tabChanged = tab != newTab;
        tab = newTab;
        submitList(items);
        if (tabChanged) {
            // The remove button names the tab.
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    LibraryItem itemAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMediaRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static int tabLabel(LibraryTab tab) {
        return switch (tab) {
            case WANT -> R.string.library_tab_want;
            case WATCHED -> R.string.library_tab_watched;
            case FAVORITES -> R.string.library_tab_favorites;
        };
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMediaRowBinding binding;
        private LibraryItem item;

        ViewHolder(ItemMediaRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.row.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                    binding.row, com.google.android.material.R.attr.colorSurface));
            binding.row.setForeground(androidx.core.content.ContextCompat.getDrawable(binding.row.getContext(),
                    resolveSelectable(binding.row.getContext())));
            binding.remove.setVisibility(View.VISIBLE);
            binding.row.setOnClickListener(v -> {
                if (item != null) {
                    onOpenMedia.accept(item.media().key());
                }
            });
            binding.remove.setOnClickListener(v -> {
                if (item != null) {
                    onRemove.accept(item);
                }
            });
        }

        void bind(LibraryItem libraryItem) {
            item = libraryItem;
            Context context = binding.getRoot().getContext();
            binding.title.setText(libraryItem.media().title());
            List<String> parts = new ArrayList<>();
            parts.add(Formats.typeLabel(context, libraryItem.media().mediaType()));
            String subtitle = Formats.subtitle(context, libraryItem.media());
            if (!subtitle.isEmpty()) {
                parts.add(subtitle);
            }
            binding.details.setText(String.join(" · ", parts));
            binding.remove.setContentDescription(
                    context.getString(R.string.library_remove, context.getString(tabLabel(tab))));
            Images.bindPoster(binding.poster, libraryItem.media().posterPath(), libraryItem.media().title(),
                    ImageSize.POSTER_LIST, libraryItem.entry().localPosterPath());
        }
    }

    private static int resolveSelectable(Context context) {
        android.util.TypedValue value = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId;
    }
}

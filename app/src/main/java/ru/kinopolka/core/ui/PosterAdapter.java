package ru.kinopolka.core.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.databinding.ItemPosterBinding;

/** Posters of a horizontal shelf. */
public final class PosterAdapter extends ListAdapter<Media, PosterAdapter.PosterViewHolder> {

    public static final DiffUtil.ItemCallback<Media> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Media oldItem, @NonNull Media newItem) {
            return oldItem.key().equals(newItem.key());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Media oldItem, @NonNull Media newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final Consumer<MediaKey> onOpenMedia;

    public PosterAdapter(Consumer<MediaKey> onOpenMedia) {
        super(DIFF);
        this.onOpenMedia = onOpenMedia;
    }

    @NonNull
    @Override
    public PosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPosterBinding binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PosterViewHolder(binding, onOpenMedia, true);
    }

    @Override
    public void onBindViewHolder(@NonNull PosterViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /** A poster card; in grids it fills the cell, in shelves it has the shelf width. */
    public static final class PosterViewHolder extends RecyclerView.ViewHolder {

        private final ItemPosterBinding binding;
        private final Consumer<MediaKey> onOpenMedia;
        private Media media;

        public PosterViewHolder(ItemPosterBinding binding, Consumer<MediaKey> onOpenMedia, boolean inShelf) {
            super(binding.getRoot());
            this.binding = binding;
            this.onOpenMedia = onOpenMedia;
            if (inShelf) {
                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                        binding.getRoot().getLayoutParams().width, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(binding.getRoot().getResources()
                        .getDimensionPixelSize(ru.kinopolka.R.dimen.shelf_spacing));
                binding.getRoot().setLayoutParams(params);
            } else {
                binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            binding.getRoot().setOnClickListener(v -> {
                if (media != null) {
                    onOpenMedia.accept(media.key());
                }
            });
        }

        public void bind(Media item) {
            media = item;
            binding.title.setText(item.title());
            binding.subtitle.setText(Formats.subtitle(binding.getRoot().getContext(), item));
            Images.bindPoster(binding.poster, item.posterPath(), item.title());
        }
    }
}

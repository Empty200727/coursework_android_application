package ru.kinopolka.feature.genre;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import java.util.function.Consumer;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.ui.PosterAdapter;
import ru.kinopolka.databinding.ItemPosterBinding;

/** Poster cards of the genre grid. */
final class GenreAdapter extends PagingDataAdapter<Media, PosterAdapter.PosterViewHolder> {

    private final Consumer<MediaKey> onOpenMedia;

    GenreAdapter(Consumer<MediaKey> onOpenMedia) {
        super(PosterAdapter.DIFF);
        this.onOpenMedia = onOpenMedia;
    }

    @NonNull
    @Override
    public PosterAdapter.PosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPosterBinding binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PosterAdapter.PosterViewHolder(binding, onOpenMedia, false);
    }

    @Override
    public void onBindViewHolder(@NonNull PosterAdapter.PosterViewHolder holder, int position) {
        Media media = getItem(position);
        if (media != null) {
            holder.bind(media);
        }
    }
}

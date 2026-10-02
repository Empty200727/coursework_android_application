package ru.kinopolka.feature.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.ui.Formats;
import ru.kinopolka.core.ui.Images;
import ru.kinopolka.core.ui.PosterAdapter;
import ru.kinopolka.databinding.ItemMediaRowBinding;

/** Result row: poster, title, year, type and 1–2 genres (docs/PLAN.md, section 7). */
final class SearchResultsAdapter extends PagingDataAdapter<Media, SearchResultsAdapter.ViewHolder> {

    private static final int MAX_ROW_GENRES = 2;

    private final Consumer<MediaKey> onOpenMedia;
    private Map<MediaType, Map<Integer, String>> genreNames = Map.of();

    SearchResultsAdapter(Consumer<MediaKey> onOpenMedia) {
        super(PosterAdapter.DIFF);
        this.onOpenMedia = onOpenMedia;
    }

    /** Genre names arrive from the catalog after the results; visible rows are rebound. */
    void setGenreNames(Map<MediaType, Map<Integer, String>> names) {
        if (!names.equals(genreNames)) {
            genreNames = names;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMediaRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Media media = getItem(position);
        if (media != null) {
            holder.bind(media);
        }
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMediaRowBinding binding;
        private Media media;

        ViewHolder(ItemMediaRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.row.setOnClickListener(v -> {
                if (media != null) {
                    onOpenMedia.accept(media.key());
                }
            });
        }

        void bind(Media item) {
            media = item;
            Context context = binding.getRoot().getContext();
            binding.title.setText(item.title());
            List<String> details = new ArrayList<>();
            if (item.releaseYear() != null) {
                details.add(String.valueOf(item.releaseYear()));
            }
            details.add(Formats.typeLabel(context, item.mediaType()));
            binding.details.setText(String.join(" · ", details));

            Map<Integer, String> names = genreNames.getOrDefault(item.mediaType(), Map.of());
            List<String> genres = new ArrayList<>();
            for (Integer id : item.genreIds()) {
                String name = names.get(id);
                if (name != null && genres.size() < MAX_ROW_GENRES) {
                    genres.add(name);
                }
            }
            binding.genres.setText(String.join(", ", genres));
            binding.genres.setVisibility(genres.isEmpty() ? View.GONE : View.VISIBLE);
            Images.bindPoster(binding.poster, item.posterPath(), item.title());
        }
    }
}

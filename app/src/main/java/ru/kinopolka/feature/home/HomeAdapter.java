package ru.kinopolka.feature.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.Shelf;
import ru.kinopolka.core.ui.FilterChips;
import ru.kinopolka.core.ui.Formats;
import ru.kinopolka.core.ui.ShelfBinder;
import ru.kinopolka.databinding.ItemHomeSearchBinding;
import ru.kinopolka.databinding.ItemMessageBinding;
import ru.kinopolka.databinding.ItemShelfBinding;
import ru.kinopolka.databinding.ItemShelfPlaceholderBinding;
import ru.kinopolka.databinding.ViewFilterChipsBinding;
import ru.kinopolka.databinding.ViewOfflineBannerBinding;
import ru.kinopolka.feature.home.HomeUiState.ShelfContent;

/** Rows of the home screen: everything scrolls together, shelves are rows with their own lists. */
final class HomeAdapter extends ListAdapter<HomeAdapter.Row, RecyclerView.ViewHolder> {

    /** One row of the home list. */
    sealed interface Row permits Offline, Search, Filter, Placeholder, Message, ShelfRow {
        String key();

        /** Rows are records: equal content means an unchanged row. */
        @Override
        boolean equals(Object other);
    }

    record Offline() implements Row {
        @Override
        public String key() {
            return "offline";
        }
    }

    record Search() implements Row {
        @Override
        public String key() {
            return "search";
        }
    }

    record Filter(MediaFilter filter) implements Row {
        @Override
        public String key() {
            return "filter";
        }
    }

    record Placeholder(int index) implements Row {
        @Override
        public String key() {
            return "placeholder-" + index;
        }
    }

    /** Full error with «Повторить», the empty state or a short refresh error over cached shelves. */
    record Message(String key, @Nullable DataError error, boolean retry) implements Row {
    }

    record ShelfRow(ShelfContent content, boolean loading) implements Row {
        @Override
        public String key() {
            return content.shelf().key();
        }
    }

    interface Callbacks {
        void onOpenSearch();

        void onFilterChange(MediaFilter filter);

        void onOpenGenre(Genre genre);

        void onOpenMedia(MediaKey key);

        void onRetry();
    }

    private static final int TYPE_OFFLINE = 0;
    private static final int TYPE_SEARCH = 1;
    private static final int TYPE_FILTER = 2;
    private static final int TYPE_PLACEHOLDER = 3;
    private static final int TYPE_MESSAGE = 4;
    private static final int TYPE_SHELF = 5;

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row oldItem, @NonNull Row newItem) {
            return oldItem.key().equals(newItem.key());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row oldItem, @NonNull Row newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final Callbacks callbacks;

    HomeAdapter(Callbacks callbacks) {
        super(DIFF);
        this.callbacks = callbacks;
    }

    @Override
    public int getItemViewType(int position) {
        Row row = getItem(position);
        if (row instanceof Offline) {
            return TYPE_OFFLINE;
        }
        if (row instanceof Search) {
            return TYPE_SEARCH;
        }
        if (row instanceof Filter) {
            return TYPE_FILTER;
        }
        if (row instanceof Placeholder) {
            return TYPE_PLACEHOLDER;
        }
        if (row instanceof Message) {
            return TYPE_MESSAGE;
        }
        return TYPE_SHELF;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return switch (viewType) {
            case TYPE_OFFLINE -> {
                ViewOfflineBannerBinding binding = ViewOfflineBannerBinding.inflate(inflater, parent, false);
                binding.getRoot().setVisibility(View.VISIBLE);
                yield new SimpleHolder(binding.getRoot());
            }
            case TYPE_SEARCH -> {
                ItemHomeSearchBinding binding = ItemHomeSearchBinding.inflate(inflater, parent, false);
                binding.searchEntry.setOnClickListener(v -> callbacks.onOpenSearch());
                yield new SimpleHolder(binding.getRoot());
            }
            case TYPE_FILTER -> new FilterHolder(ViewFilterChipsBinding.inflate(inflater, parent, false), callbacks);
            case TYPE_PLACEHOLDER -> new SimpleHolder(
                    ItemShelfPlaceholderBinding.inflate(inflater, parent, false).getRoot());
            case TYPE_MESSAGE -> new MessageHolder(ItemMessageBinding.inflate(inflater, parent, false), callbacks);
            default -> new ShelfHolder(ItemShelfBinding.inflate(inflater, parent, false), callbacks);
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);
        if (holder instanceof FilterHolder filterHolder) {
            filterHolder.bind(((Filter) row).filter());
        } else if (holder instanceof MessageHolder messageHolder) {
            messageHolder.bind((Message) row);
        } else if (holder instanceof ShelfHolder shelfHolder) {
            shelfHolder.bind((ShelfRow) row);
        }
    }

    private static final class SimpleHolder extends RecyclerView.ViewHolder {
        SimpleHolder(View view) {
            super(view);
        }
    }

    private static final class FilterHolder extends RecyclerView.ViewHolder {
        private final ViewFilterChipsBinding binding;

        FilterHolder(ViewFilterChipsBinding binding, Callbacks callbacks) {
            super(binding.getRoot());
            this.binding = binding;
            FilterChips.setup(binding, callbacks::onFilterChange);
        }

        void bind(MediaFilter filter) {
            FilterChips.select(binding, filter);
        }
    }

    private static final class MessageHolder extends RecyclerView.ViewHolder {
        private final ItemMessageBinding binding;

        MessageHolder(ItemMessageBinding binding, Callbacks callbacks) {
            super(binding.getRoot());
            this.binding = binding;
            binding.messageAction.setOnClickListener(v -> callbacks.onRetry());
        }

        void bind(Message message) {
            Context context = binding.getRoot().getContext();
            binding.message.setText(message.error() == null
                    ? context.getString(R.string.home_empty)
                    : context.getString(Formats.errorMessage(message.error())));
            // A refresh error over cached shelves is a short line in the error color.
            boolean shortError = message.error() != null && !message.retry();
            binding.message.setTextColor(MaterialColors.getColor(binding.message, shortError
                    ? androidx.appcompat.R.attr.colorError
                    : com.google.android.material.R.attr.colorOnSurface));
            int padding = context.getResources().getDimensionPixelSize(
                    shortError ? R.dimen.message_padding_short : R.dimen.message_padding);
            binding.getRoot().setPadding(padding, padding, padding, padding);
            binding.messageAction.setVisibility(message.retry() ? View.VISIBLE : View.GONE);
        }
    }

    private static final class ShelfHolder extends RecyclerView.ViewHolder {
        private final ShelfBinder binder;
        private final Callbacks callbacks;
        private final Context context;

        ShelfHolder(ItemShelfBinding binding, Callbacks callbacks) {
            super(binding.getRoot());
            this.binder = new ShelfBinder(binding, callbacks::onOpenMedia);
            this.callbacks = callbacks;
            this.context = binding.getRoot().getContext();
        }

        void bind(ShelfRow row) {
            Shelf shelf = row.content().shelf();
            Runnable onSeeAll = null;
            if (shelf instanceof Shelf.ByGenre byGenre) {
                onSeeAll = () -> callbacks.onOpenGenre(byGenre.genre());
            }
            binder.bind(title(shelf), row.content().items(), row.loading(), onSeeAll);
        }

        private String title(Shelf shelf) {
            if (shelf instanceof Shelf.Trending) {
                return context.getString(R.string.home_trending);
            }
            if (shelf instanceof Shelf.Popular popular) {
                return context.getString(popular.mediaType() == MediaType.MOVIE
                        ? R.string.home_popular_movies : R.string.home_popular_series);
            }
            return ((Shelf.ByGenre) shelf).genre().name();
        }
    }
}

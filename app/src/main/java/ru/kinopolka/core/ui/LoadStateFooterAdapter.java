package ru.kinopolka.core.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;
import androidx.recyclerview.widget.RecyclerView;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.databinding.ItemLoadStateBinding;

/** End of a paged list: progress while the next page loads, a compact error with «Повторить». */
public final class LoadStateFooterAdapter extends LoadStateAdapter<LoadStateFooterAdapter.ViewHolder> {

    private final Runnable onRetry;

    public LoadStateFooterAdapter(Runnable onRetry) {
        this.onRetry = onRetry;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @NonNull LoadState loadState) {
        ItemLoadStateBinding binding =
                ItemLoadStateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.loadRetry.setOnClickListener(v -> onRetry.run());
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @NonNull LoadState loadState) {
        ItemLoadStateBinding binding = holder.binding;
        boolean error = loadState instanceof LoadState.Error;
        binding.loadProgress.setVisibility(loadState instanceof LoadState.Loading ? View.VISIBLE : View.GONE);
        binding.loadError.setVisibility(error ? View.VISIBLE : View.GONE);
        if (error) {
            DataError dataError = DataError.of(((LoadState.Error) loadState).getError());
            binding.loadErrorMessage.setText(Formats.errorMessage(dataError));
        }
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoadStateBinding binding;

        ViewHolder(ItemLoadStateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

package ru.kinopolka.feature.genre;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ru.kinopolka.databinding.ItemPosterSkeletonBinding;

/** Poster skeletons shown in the grid until the first page arrives (N-06). */
final class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.ViewHolder> {

    private final int count;
    private boolean visible = true;

    SkeletonAdapter(int count) {
        this.count = count;
    }

    void setVisible(boolean newVisible) {
        if (visible == newVisible) {
            return;
        }
        visible = newVisible;
        if (visible) {
            notifyItemRangeInserted(0, count);
        } else {
            notifyItemRangeRemoved(0, count);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = ItemPosterSkeletonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                .getRoot();
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Skeletons have no content.
    }

    @Override
    public int getItemCount() {
        return visible ? count : 0;
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View view) {
            super(view);
        }
    }
}

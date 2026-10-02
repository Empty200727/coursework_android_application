package ru.kinopolka.feature.genre;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Equal gaps around the cells of the grid. */
final class GridSpacing extends RecyclerView.ItemDecoration {

    private final int half;

    GridSpacing(int spacing) {
        this.half = spacing / 2;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        outRect.set(half, half, half, half);
    }
}

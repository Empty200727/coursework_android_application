package ru.kinopolka.feature.library;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import java.util.function.IntConsumer;
import ru.kinopolka.R;

/** F-13: a swipe in either direction removes the title; the row button does the same. */
final class SwipeToRemove extends ItemTouchHelper.SimpleCallback {

    private final IntConsumer onSwiped;

    SwipeToRemove(IntConsumer onSwiped) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.onSwiped = onSwiped;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            onSwiped.accept(position);
        }
    }

    /** The red background with the delete icon under the moving row. */
    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        android.view.View row = viewHolder.itemView;
        if (dX != 0) {
            ColorDrawable background = new ColorDrawable(MaterialColors.getColor(row,
                    com.google.android.material.R.attr.colorErrorContainer));
            int left = dX > 0 ? row.getLeft() : row.getRight() + (int) dX;
            int right = dX > 0 ? row.getLeft() + (int) dX : row.getRight();
            background.setBounds(left, row.getTop(), right, row.getBottom());
            background.draw(canvas);

            Drawable icon = ContextCompat.getDrawable(row.getContext(), R.drawable.ic_delete);
            if (icon != null) {
                icon.mutate().setTint(MaterialColors.getColor(row,
                        com.google.android.material.R.attr.colorOnErrorContainer));
                int size = icon.getIntrinsicHeight();
                int margin = row.getResources().getDimensionPixelSize(R.dimen.swipe_icon_margin);
                int top = row.getTop() + (row.getHeight() - size) / 2;
                int iconLeft = dX > 0 ? row.getLeft() + margin : row.getRight() - margin - size;
                icon.setBounds(iconLeft, top, iconLeft + size, top + size);
                if (Math.abs(dX) > margin + size) {
                    icon.draw(canvas);
                }
            }
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}

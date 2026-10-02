package ru.kinopolka.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import ru.kinopolka.R;

/** Frame whose height follows its width: 2:3 posters, 16:9 backdrops. */
public class AspectRatioFrameLayout extends FrameLayout {

    private static final float POSTER_ASPECT_RATIO = 2f / 3f;

    private final float aspectRatio;

    public AspectRatioFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout);
        try {
            aspectRatio = array.getFloat(R.styleable.AspectRatioFrameLayout_aspectRatio, POSTER_ASPECT_RATIO);
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width / aspectRatio);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}

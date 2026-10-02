package ru.kinopolka.core.ui;

import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import java.io.File;
import ru.kinopolka.BuildConfig;
import ru.kinopolka.core.network.ImageSize;
import ru.kinopolka.core.network.TmdbNetwork;
import ru.kinopolka.databinding.ViewPosterBinding;

/** Image loading with Glide; its default disk cache holds 250 MB (N-03). */
public final class Images {

    private Images() {
    }

    /**
     * Poster with the title on the placeholder. A poster saved for the library is shown from the
     * file, also without network (N-01).
     */
    public static void bindPoster(ViewPosterBinding binding, @Nullable String posterPath, String title,
            ImageSize size, @Nullable String localPath) {
        binding.posterPlaceholder.setText(title);
        Object model = null;
        if (localPath != null && new File(localPath).isFile()) {
            model = new File(localPath);
        } else {
            model = TmdbNetwork.imageUrl(posterPath, size, BuildConfig.TMDB_IMAGE_BASE_URL);
        }
        load(binding.posterImage, model);
    }

    public static void bindPoster(ViewPosterBinding binding, @Nullable String posterPath, String title) {
        bindPoster(binding, posterPath, title, ImageSize.POSTER_LIST, null);
    }

    public static void load(ImageView view, @Nullable String path, ImageSize size) {
        load(view, TmdbNetwork.imageUrl(path, size, BuildConfig.TMDB_IMAGE_BASE_URL));
    }

    private static void load(ImageView view, @Nullable Object model) {
        if (model == null) {
            Glide.with(view).clear(view);
            view.setImageDrawable(null);
            return;
        }
        Glide.with(view).load(model).centerCrop().into(view);
    }
}

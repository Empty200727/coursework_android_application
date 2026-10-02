package ru.kinopolka.navigation;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import ru.kinopolka.R;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;

/**
 * Screen transitions (docs/PLAN.md, section 7). Only primitive arguments are passed: the type and
 * id of a title, the key of a genre; screens read the data from the repositories.
 */
public final class Navigator {

    public static final String ARG_MEDIA_TYPE = "mediaType";
    public static final String ARG_ID = "id";
    public static final String ARG_GENRE_KEY = "genreKey";
    public static final String ARG_FILTER = "filter";

    private Navigator() {
    }

    public static Bundle detailsArgs(MediaKey key) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_TYPE, key.mediaType().key());
        args.putInt(ARG_ID, key.tmdbId());
        return args;
    }

    /** Reads the arguments of the title card; an unknown type falls back to a movie. */
    public static MediaKey mediaKey(String mediaTypeKey, int id) {
        MediaType type = MediaType.fromKey(mediaTypeKey);
        return new MediaKey(type != null ? type : MediaType.MOVIE, id);
    }

    public static Bundle genreArgs(String genreKey, MediaFilter filter) {
        Bundle args = new Bundle();
        args.putString(ARG_GENRE_KEY, genreKey);
        args.putString(ARG_FILTER, filter.name());
        return args;
    }

    public static void openDetails(Fragment fragment, MediaKey key) {
        controller(fragment).navigate(R.id.detailsFragment, detailsArgs(key));
    }

    public static void openGenre(Fragment fragment, String genreKey, MediaFilter filter) {
        controller(fragment).navigate(R.id.genreFragment, genreArgs(genreKey, filter));
    }

    public static void openAbout(Fragment fragment) {
        controller(fragment).navigate(R.id.aboutFragment);
    }

    public static void back(Fragment fragment) {
        controller(fragment).navigateUp();
    }

    /**
     * Switches tabs keeping a separate back stack for each, exactly like the bottom navigation bar:
     * the stack of the tab being left is saved, the stack of the selected tab is restored.
     */
    public static void openTab(Fragment fragment, @IdRes int graphId) {
        NavController controller = controller(fragment);
        NavGraph graph = controller.getGraph();
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(NavGraph.findStartDestination(graph).getId(), false, true)
                .build();
        controller.navigate(graphId, null, options);
    }

    private static NavController controller(Fragment fragment) {
        return NavHostFragment.findNavController(fragment);
    }
}

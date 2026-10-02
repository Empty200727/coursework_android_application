package ru.kinopolka.feature.details;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.MaterialColors;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.kinopolka.R;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.model.WatchStatus;
import ru.kinopolka.core.network.ImageSize;
import ru.kinopolka.core.ui.Formats;
import ru.kinopolka.core.ui.Images;
import ru.kinopolka.core.ui.ShelfBinder;
import ru.kinopolka.core.ui.StateViews;
import ru.kinopolka.databinding.FragmentDetailsBinding;
import ru.kinopolka.navigation.Navigator;

/** Title card (F-08…F-11). */
@AndroidEntryPoint
public class DetailsFragment extends Fragment {

    private static final int COLLAPSED_OVERVIEW_LINES = 4;
    private static final String OVERVIEW_EXPANDED_KEY = "overviewExpanded";

    private DetailsViewModel viewModel;
    private FragmentDetailsBinding binding;
    private CastAdapter castAdapter;
    private ShelfBinder relatedShelf;
    private boolean overviewExpanded;
    /** Set while the status buttons are updated from the state, so the change is not sent back. */
    private boolean bindingStatus;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DetailsViewModel.class);
        overviewExpanded = savedInstanceState != null && savedInstanceState.getBoolean(OVERVIEW_EXPANDED_KEY);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.toolbar.setNavigationOnClickListener(v -> Navigator.back(this));
        castAdapter = new CastAdapter();
        binding.cast.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.cast.setAdapter(castAdapter);
        relatedShelf = new ShelfBinder(binding.related, key -> Navigator.openDetails(this, key));

        binding.statusWant.setOnClickListener(v -> onStatusButton(WatchStatus.WANT));
        binding.statusWatched.setOnClickListener(v -> onStatusButton(WatchStatus.WATCHED));
        binding.favorite.setOnClickListener(v -> viewModel.onFavoriteClick());
        binding.overviewToggle.setOnClickListener(v -> {
            overviewExpanded = !overviewExpanded;
            renderOverviewLines();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(OVERVIEW_EXPANDED_KEY, overviewExpanded);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        castAdapter = null;
        relatedShelf = null;
        binding = null;
    }

    private void onStatusButton(WatchStatus status) {
        if (!bindingStatus) {
            viewModel.onStatusClick(status);
        }
    }

    private void render(DetailsUiState state) {
        MediaDetails details = state.details();
        binding.toolbar.setTitle(details != null ? details.media().title() : getString(R.string.details_title));
        StateViews.setVisible(binding.offlineBanner.getRoot(), state.offline());
        DetailsUiState.Content content = state.content();
        StateViews.setVisible(binding.skeleton.getRoot(), content == DetailsUiState.Content.LOADING);
        StateViews.setVisible(binding.content, content == DetailsUiState.Content.DATA);
        if (content == DetailsUiState.Content.ERROR) {
            StateViews.showError(binding.state, Objects.requireNonNull(state.error()), viewModel::retry);
        } else {
            StateViews.hide(binding.state);
        }
        if (details != null) {
            renderDetails(state, details);
        }
    }

    private void renderDetails(DetailsUiState state, MediaDetails details) {
        Context context = requireContext();
        Media media = details.media();
        boolean loadingExtras = state.refreshing() && !details.complete();

        Images.load(binding.backdrop, media.backdropPath(), ImageSize.BACKDROP);
        Images.bindPoster(binding.poster, media.posterPath(), media.title(), ImageSize.POSTER_DETAILS,
                state.entry() != null ? state.entry().localPosterPath() : null);
        binding.title.setText(media.title());
        boolean showOriginal = media.originalTitle() != null && !media.originalTitle().equals(media.title());
        binding.originalTitle.setText(showOriginal ? media.originalTitle() : null);
        StateViews.setVisible(binding.originalTitle, showOriginal);
        binding.meta.setText(metaLine(context, details));
        String rating = ratingLine(context, media);
        binding.rating.setText(rating);
        StateViews.setVisible(binding.rating, rating != null);
        List<String> genres = new ArrayList<>();
        for (TmdbGenre genre : details.genres()) {
            genres.add(genre.name());
        }
        binding.genres.setText(String.join(", ", genres));
        StateViews.setVisible(binding.genres, !genres.isEmpty());

        renderLibraryActions(state);
        renderOverview(details, loadingExtras);

        castAdapter.submitList(details.cast());
        StateViews.setVisible(binding.castSection, !details.cast().isEmpty() || loadingExtras);
        StateViews.setVisible(binding.castSkeleton.getRoot(), details.cast().isEmpty() && loadingExtras);
        StateViews.setVisible(binding.cast, !details.cast().isEmpty());

        List<Media> related = details.related();
        StateViews.setVisible(binding.related.getRoot(), !related.isEmpty() || loadingExtras);
        relatedShelf.bind(getString(R.string.details_related), related, loadingExtras, null);
    }

    /** F-11: «Хочу посмотреть» / «Смотрел» exclude each other, «В избранное» is separate. */
    private void renderLibraryActions(DetailsUiState state) {
        bindingStatus = true;
        WatchStatus status = state.watchStatus();
        if (status == WatchStatus.NONE) {
            binding.statusGroup.clearChecked();
        } else {
            binding.statusGroup.check(status == WatchStatus.WANT ? R.id.status_want : R.id.status_watched);
        }
        bindingStatus = false;
        boolean favorite = state.favorite();
        binding.favorite.setIconResource(favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        binding.favorite.setIconTint(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                binding.favorite, favorite
                        ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurfaceVariant)));
        binding.favorite.setContentDescription(getString(
                favorite ? R.string.details_favorite_remove : R.string.details_favorite_add));
        binding.favorite.setSelected(favorite);
    }

    /** Overview collapsed to a few lines with «Ещё» (docs/PLAN.md, section 7). */
    private void renderOverview(MediaDetails details, boolean loading) {
        String overview = details.media().overview();
        StateViews.setVisible(binding.overviewEnglish, overview != null && details.overviewFallback());
        StateViews.setVisible(binding.overviewSkeleton, overview == null && loading);
        StateViews.setVisible(binding.overview, overview != null || !loading);
        if (overview != null) {
            binding.overview.setText(overview);
            binding.overview.setTextColor(MaterialColors.getColor(binding.overview,
                    com.google.android.material.R.attr.colorOnSurface));
            renderOverviewLines();
        } else {
            binding.overview.setText(R.string.details_no_overview);
            binding.overview.setTextColor(MaterialColors.getColor(binding.overview,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
            binding.overview.setMaxLines(Integer.MAX_VALUE);
            binding.overviewToggle.setVisibility(View.GONE);
        }
    }

    private void renderOverviewLines() {
        binding.overview.setMaxLines(overviewExpanded ? Integer.MAX_VALUE : COLLAPSED_OVERVIEW_LINES);
        binding.overviewToggle.setText(
                overviewExpanded ? R.string.details_overview_less : R.string.details_overview_more);
        // The line count is known after layout.
        binding.overview.post(() -> {
            if (binding == null || binding.overview.getLayout() == null) {
                return;
            }
            int lines = binding.overview.getLayout().getLineCount();
            boolean overflows = lines > COLLAPSED_OVERVIEW_LINES
                    || lines > 0 && binding.overview.getLayout().getEllipsisCount(lines - 1) > 0;
            binding.overviewToggle.setVisibility(overflows || overviewExpanded ? View.VISIBLE : View.GONE);
        });
    }

    /** «2008–2013 · Сериал · 5 сезонов» or «1999 · Фильм · 2 ч 19 мин». */
    static String metaLine(Context context, MediaDetails details) {
        List<String> parts = new ArrayList<>();
        String years = DetailsFormat.yearsText(details, context.getString(R.string.details_ongoing));
        if (years != null) {
            parts.add(years);
        }
        parts.add(Formats.typeLabel(context, details.media().mediaType()));
        if (details.media().mediaType() == MediaType.MOVIE && details.runtimeMinutes() != null) {
            int hours = details.runtimeMinutes() / DetailsFormat.MINUTES_IN_HOUR;
            int minutes = details.runtimeMinutes() % DetailsFormat.MINUTES_IN_HOUR;
            parts.add(hours > 0
                    ? context.getString(R.string.details_runtime, hours, minutes)
                    : context.getString(R.string.details_runtime_minutes, minutes));
        } else if (details.media().mediaType() == MediaType.TV && details.numberOfSeasons() != null) {
            int seasons = details.numberOfSeasons();
            parts.add(context.getResources().getQuantityString(R.plurals.details_seasons, seasons, seasons));
        }
        return String.join(" · ", parts);
    }

    /** «★ 8.4 · 29 870 оценок». */
    @Nullable
    static String ratingLine(Context context, Media media) {
        if (media.voteCount() <= 0) {
            return null;
        }
        String votes = context.getResources().getQuantityString(R.plurals.details_votes, media.voteCount(),
                DetailsFormat.formatCount(media.voteCount()));
        return context.getString(R.string.rating_short, Formats.rating(media.voteAverage())) + " · " + votes;
    }
}

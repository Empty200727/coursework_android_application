package ru.kinopolka.feature.genre;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaSort;
import ru.kinopolka.core.ui.Formats;
import ru.kinopolka.core.ui.LoadStateFooterAdapter;
import ru.kinopolka.core.ui.StateViews;
import ru.kinopolka.databinding.FragmentGenreBinding;
import ru.kinopolka.navigation.Navigator;

/** Full list of a genre (F-06): grid, infinite scrolling, sorting. */
@AndroidEntryPoint
public class GenreFragment extends Fragment {

    private static final int SKELETON_ITEMS = 9;

    private GenreViewModel viewModel;
    private FragmentGenreBinding binding;
    private GenreAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    @Nullable
    private CombinedLoadStates loadStates;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GenreViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentGenreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.toolbar.setNavigationOnClickListener(v -> Navigator.back(this));

        adapter = new GenreAdapter(key -> Navigator.openDetails(this, key));
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        skeletonAdapter = new SkeletonAdapter(SKELETON_ITEMS);
        LoadStateFooterAdapter footer = new LoadStateFooterAdapter(adapter::retry);
        ConcatAdapter concat = new ConcatAdapter(skeletonAdapter, adapter.withLoadStateFooter(footer));

        int cell = getResources().getDimensionPixelSize(R.dimen.grid_min_cell);
        int width = getResources().getDisplayMetrics().widthPixels;
        int columns = Math.max(2, width / cell);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), columns);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // The footer with progress or error takes a whole line.
                int posters = skeletonAdapter.getItemCount() + adapter.getItemCount();
                return position >= posters ? columns : 1;
            }
        });
        binding.grid.setLayoutManager(layoutManager);
        binding.grid.addItemDecoration(new GridSpacing(getResources().getDimensionPixelSize(R.dimen.grid_spacing)));
        binding.grid.setAdapter(concat);

        binding.sortGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.contains(R.id.sort_popular)) {
                viewModel.onSortChange(MediaSort.POPULARITY);
            } else if (ids.contains(R.id.sort_rating)) {
                viewModel.onSortChange(MediaSort.RATING);
            } else if (ids.contains(R.id.sort_newest)) {
                viewModel.onSortChange(MediaSort.NEWEST);
            }
        });

        viewModel.getMedia().observe(getViewLifecycleOwner(),
                data -> adapter.submitData(getViewLifecycleOwner().getLifecycle(), data));
        // Items may arrive after the load state: both change what is shown.
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                render();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                render();
            }
        });
        adapter.addLoadStateListener(states -> {
            loadStates = states;
            render();
            return Unit.INSTANCE;
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            String title = state.title() != null ? state.title() : getString(R.string.genre_title);
            binding.toolbar.setTitle(state.filter() == MediaFilter.ALL
                    ? title
                    : getString(R.string.genre_title_with_filter, title,
                            getString(Formats.filterLabel(state.filter()))));
            StateViews.setVisible(binding.offlineBanner.getRoot(), state.offline());
            int sortId = switch (state.sort()) {
                case POPULARITY -> R.id.sort_popular;
                case RATING -> R.id.sort_rating;
                case NEWEST -> R.id.sort_newest;
            };
            if (binding.sortGroup.getCheckedChipId() != sortId) {
                binding.sortGroup.check(sortId);
            }
        });
        render();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        skeletonAdapter = null;
        binding = null;
    }

    private void render() {
        if (binding == null) {
            return;
        }
        boolean empty = adapter.getItemCount() == 0;
        LoadState refresh = loadStates == null ? null : loadStates.getRefresh();
        boolean endReached = loadStates != null && loadStates.getAppend().getEndOfPaginationReached();
        if (refresh instanceof LoadState.Error error && empty) {
            StateViews.showError(binding.state, DataError.of(error.getError()), adapter::retry);
            binding.grid.setVisibility(View.INVISIBLE);
        } else if (refresh instanceof LoadState.NotLoading && empty && endReached) {
            StateViews.showMessage(binding.state, getString(R.string.genre_empty));
            binding.grid.setVisibility(View.INVISIBLE);
        } else {
            // No errors and no titles yet means loading: skeleton posters fill the grid.
            StateViews.hide(binding.state);
            binding.grid.setVisibility(View.VISIBLE);
            skeletonAdapter.setVisible(empty);
        }
    }
}

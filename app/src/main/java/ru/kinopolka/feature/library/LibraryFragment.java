package ru.kinopolka.feature.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import dagger.hilt.android.AndroidEntryPoint;
import ru.kinopolka.R;
import ru.kinopolka.core.model.LibrarySort;
import ru.kinopolka.core.model.LibraryTab;
import ru.kinopolka.core.ui.FilterChips;
import ru.kinopolka.core.ui.StateViews;
import ru.kinopolka.databinding.FragmentLibraryBinding;
import ru.kinopolka.feature.library.LibraryUiState.PendingUndo;
import ru.kinopolka.navigation.MainViewModel;
import ru.kinopolka.navigation.Navigator;

/** «Моя полка» (F-12, F-13). */
@AndroidEntryPoint
public class LibraryFragment extends Fragment {

    private LibraryViewModel viewModel;
    private FragmentLibraryBinding binding;
    private LibraryAdapter adapter;
    @Nullable
    private PendingUndo shownUndo;
    @Nullable
    private Snackbar snackbar;
    /** Set while tabs are updated from the state, so the selection is not sent back. */
    private boolean bindingTabs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.toolbar.inflateMenu(R.menu.library);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItem);

        for (LibraryTab tab : LibraryTab.values()) {
            binding.tabs.addTab(binding.tabs.newTab().setText(LibraryAdapter.tabLabel(tab)).setTag(tab));
        }
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!bindingTabs) {
                    viewModel.onTabChange((LibraryTab) tab.getTag());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Only the new selection matters.
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Nothing to change.
            }
        });
        FilterChips.setup(binding.filterChips, viewModel::onFilterChange);

        adapter = new LibraryAdapter(key -> Navigator.openDetails(this, key), viewModel::onRemove);
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.items.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.items.setAdapter(adapter);
        new ItemTouchHelper(new SwipeToRemove(position -> viewModel.onRemove(adapter.itemAt(position))))
                .attachToRecyclerView(binding.items);

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        snackbar = null;
        shownUndo = null;
        adapter = null;
        binding = null;
    }

    private boolean onMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            Navigator.openAbout(this);
            return true;
        }
        LibrarySort sort = id == R.id.sort_added ? LibrarySort.ADDED
                : id == R.id.sort_title ? LibrarySort.TITLE
                : id == R.id.sort_rating ? LibrarySort.RATING
                : null;
        if (sort != null) {
            viewModel.onSortChange(sort);
            return true;
        }
        return false;
    }

    private void render(LibraryUiState state) {
        bindingTabs = true;
        for (LibraryTab tab : LibraryTab.values()) {
            TabLayout.Tab view = binding.tabs.getTabAt(tab.ordinal());
            if (view != null) {
                view.setText(getString(R.string.library_tab_count, getString(LibraryAdapter.tabLabel(tab)),
                        state.count(tab)));
                if (tab == state.tab() && !view.isSelected()) {
                    view.select();
                }
            }
        }
        bindingTabs = false;
        FilterChips.select(binding.filterChips, state.filter());
        MenuItem sortItem = binding.toolbar.getMenu().findItem(switch (state.sort()) {
            case ADDED -> R.id.sort_added;
            case TITLE -> R.id.sort_title;
            case RATING -> R.id.sort_rating;
        });
        if (sortItem != null) {
            sortItem.setChecked(true);
        }

        adapter.submit(state.items(), state.tab());
        if (!state.loading() && state.items().isEmpty()) {
            StateViews.show(binding.state, getString(emptyMessage(state.tab())),
                    getString(R.string.library_find_something), this::findSomething);
        } else {
            StateViews.hide(binding.state);
        }
        showUndo(state.pendingUndo());
    }

    private void showUndo(@Nullable PendingUndo undo) {
        if (undo == null || undo.equals(shownUndo)) {
            return;
        }
        shownUndo = undo;
        String message = getString(R.string.library_removed, undo.title(),
                getString(LibraryAdapter.tabLabel(undo.tab())));
        Snackbar bar = Snackbar.make(binding.coordinator, message, Snackbar.LENGTH_SHORT)
                .setAction(R.string.action_undo, v -> viewModel.onUndo());
        bar.addCallback(new BaseTransientBottomBar.BaseCallback<>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                // A new removal replaces the Snackbar: the view model has already handled the old one.
                if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE) {
                    viewModel.onUndoDismissed();
                }
            }
        });
        snackbar = bar;
        bar.show();
    }

    private void findSomething() {
        new ViewModelProvider(requireActivity()).get(MainViewModel.class).requestSearchFocus();
        Navigator.openTab(this, R.id.search_graph);
    }

    private static int emptyMessage(LibraryTab tab) {
        return switch (tab) {
            case WANT -> R.string.library_empty_want;
            case WATCHED -> R.string.library_empty_watched;
            case FAVORITES -> R.string.library_empty_favorites;
        };
    }
}

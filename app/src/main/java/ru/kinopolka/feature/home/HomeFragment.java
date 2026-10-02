package ru.kinopolka.feature.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import ru.kinopolka.R;
import ru.kinopolka.core.model.Genre;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.databinding.FragmentHomeBinding;
import ru.kinopolka.feature.home.HomeAdapter.Row;
import ru.kinopolka.feature.home.HomeUiState.ShelfContent;
import ru.kinopolka.navigation.MainViewModel;
import ru.kinopolka.navigation.Navigator;

/** «Главная» (F-02, F-05, F-07). */
@AndroidEntryPoint
public class HomeFragment extends Fragment implements HomeAdapter.Callbacks {

    private static final int SKELETON_SHELVES = 4;

    private HomeViewModel viewModel;
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HomeAdapter adapter = new HomeAdapter(this);
        // The scroll position survives rotation and process death (N-05) once rows are back.
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.homeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.homeList.setAdapter(adapter);
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> adapter.submitList(rows(state)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static List<Row> rows(HomeUiState state) {
        List<Row> rows = new ArrayList<>();
        if (state.offline()) {
            rows.add(new HomeAdapter.Offline());
        }
        rows.add(new HomeAdapter.Search());
        rows.add(new HomeAdapter.Filter(state.filter()));
        switch (state.content()) {
            case LOADING -> {
                for (int i = 0; i < SKELETON_SHELVES; i++) {
                    rows.add(new HomeAdapter.Placeholder(i));
                }
            }
            case ERROR -> rows.add(new HomeAdapter.Message("error", state.error(), true));
            case EMPTY -> rows.add(new HomeAdapter.Message("empty", null, false));
            case DATA -> {
                if (state.error() != null && !state.offline()) {
                    rows.add(new HomeAdapter.Message("refreshError", state.error(), false));
                }
                for (ShelfContent content : state.shelves()) {
                    // Empty shelves are hidden once loading is over.
                    if (!content.items().isEmpty() || state.refreshing()) {
                        rows.add(new HomeAdapter.ShelfRow(content, state.refreshing()));
                    }
                }
            }
        }
        return rows;
    }

    @Override
    public void onOpenSearch() {
        new ViewModelProvider(requireActivity()).get(MainViewModel.class).requestSearchFocus();
        Navigator.openTab(this, R.id.search_graph);
    }

    @Override
    public void onFilterChange(MediaFilter filter) {
        viewModel.onFilterChange(filter);
    }

    @Override
    public void onOpenGenre(Genre genre) {
        HomeUiState state = viewModel.getUiState().getValue();
        Navigator.openGenre(this, genre.key(), state != null ? state.filter() : MediaFilter.ALL);
    }

    @Override
    public void onOpenMedia(MediaKey key) {
        Navigator.openDetails(this, key);
    }

    @Override
    public void onRetry() {
        viewModel.retry();
    }
}

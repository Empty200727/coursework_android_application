package ru.kinopolka.feature.search;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.core.ui.FilterChips;
import ru.kinopolka.core.ui.LoadStateFooterAdapter;
import ru.kinopolka.core.ui.StateViews;
import ru.kinopolka.databinding.FragmentSearchBinding;
import ru.kinopolka.navigation.MainViewModel;
import ru.kinopolka.navigation.Navigator;

/** «Поиск» (F-01…F-03). */
@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private FragmentSearchBinding binding;
    private SearchResultsAdapter adapter;
    @Nullable
    private CombinedLoadStates loadStates;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new SearchResultsAdapter(key -> Navigator.openDetails(this, key));
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.results.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.results.setAdapter(adapter.withLoadStateFooter(new LoadStateFooterAdapter(adapter::retry)));

        SearchUiState initial = viewModel.getUiState().getValue();
        if (initial != null) {
            binding.searchInput.setText(initial.query());
        }
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Only the final text matters.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onQueryChange(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Handled in onTextChanged.
            }
        });
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        FilterChips.setup(binding.filterChips, viewModel::onFilterChange);

        viewModel.getResults().observe(getViewLifecycleOwner(),
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
            FilterChips.select(binding.filterChips, state.filter());
            adapter.setGenreNames(state.genreNames());
            render();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        if (mainViewModel.consumeSearchFocus()) {
            binding.searchInput.requestFocus();
            WindowCompat.getInsetsController(requireActivity().getWindow(), binding.searchInput)
                    .show(WindowInsetsCompat.Type.ime());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        binding = null;
    }

    /** Picks one of: hint, skeleton, error, empty result, results. */
    private void render() {
        SearchUiState state = viewModel.getUiState().getValue();
        if (binding == null || state == null) {
            return;
        }
        Context context = requireContext();
        StateViews.setVisible(binding.offlineBanner.getRoot(), state.offline());
        boolean empty = adapter.getItemCount() == 0;
        LoadState refresh = loadStates == null ? null : loadStates.getRefresh();

        boolean skeleton = false;
        boolean showList = false;
        if (state.isQueryTooShort()) {
            StateViews.showMessage(binding.state, context.getString(R.string.search_min_length));
        } else if (state.isPending() || empty && (refresh == null || refresh instanceof LoadState.Loading)) {
            StateViews.hide(binding.state);
            skeleton = true;
        } else if (refresh instanceof LoadState.Error error && empty) {
            DataError dataError = DataError.of(error.getError());
            if (dataError == DataError.NO_CONNECTION) {
                // N-02: search needs the network, cached data cannot help here.
                StateViews.show(binding.state, context.getString(R.string.search_offline),
                        context.getString(R.string.action_retry), adapter::retry);
            } else {
                StateViews.showError(binding.state, dataError, adapter::retry);
            }
        } else if (refresh instanceof LoadState.NotLoading && empty) {
            StateViews.showMessage(binding.state, context.getString(R.string.search_empty));
        } else {
            StateViews.hide(binding.state);
            showList = true;
        }
        StateViews.setVisible(binding.skeleton.getRoot(), skeleton);
        binding.results.setVisibility(showList ? View.VISIBLE : View.INVISIBLE);
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = requireContext().getSystemService(InputMethodManager.class);
        keyboard.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
    }
}

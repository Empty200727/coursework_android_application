package ru.kinopolka.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.data.repository.GenreRepository

/**
 * Home screen of iteration 1: shows the unified genre catalog (F-04). Collections and
 * shelves are added in iteration 2.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(private val genreRepository: GenreRepository) : ViewModel() {

    /** `null` while a refresh is running. */
    private val refreshResult = MutableStateFlow<RefreshResult?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        genreRepository.observeGenres(),
        refreshResult,
    ) { genres, refresh ->
        val error = (refresh as? RefreshResult.Failed)?.error
        when {
            genres.isNotEmpty() -> HomeUiState.Success(genres, refreshError = error)
            error != null -> HomeUiState.Error(error)
            else -> HomeUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState.Loading,
    )

    init {
        refresh()
    }

    fun retry() = refresh()

    private fun refresh() {
        viewModelScope.launch {
            refreshResult.value = null
            refreshResult.value = genreRepository.refresh()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

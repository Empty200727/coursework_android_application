package ru.kinopolka.feature.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.navigation.GenreRoute

data class GenreUiState(
    /** `null` until the genre is found in the catalog. */
    val title: String?,
    val filter: MediaFilter,
)

/** Genre screen of iteration 1: resolves the genre name; the grid is added in iteration 2. */
@HiltViewModel
class GenreViewModel @Inject constructor(savedStateHandle: SavedStateHandle, genreRepository: GenreRepository) :
    ViewModel() {

    private val route: GenreRoute = savedStateHandle.toRoute()

    val uiState: StateFlow<GenreUiState> = genreRepository.observeGenres()
        .map { genres ->
            GenreUiState(
                title = genres.firstOrNull { it.key == route.genreKey }?.name,
                filter = route.mediaFilter,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GenreUiState(title = null, filter = route.mediaFilter),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

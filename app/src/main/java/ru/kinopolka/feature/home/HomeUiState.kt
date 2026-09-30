package ru.kinopolka.feature.home

import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.model.Genre

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [refreshError] is set when cached genres are shown but the update failed. */
    data class Success(val genres: List<Genre>, val refreshError: DataError? = null) : HomeUiState

    /** Nothing is cached and loading failed. */
    data class Error(val error: DataError) : HomeUiState
}

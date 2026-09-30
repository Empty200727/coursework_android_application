package ru.kinopolka.feature.genre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kinopolka.R
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent

/** Genre screen (F-06): the paged grid with sorting is implemented in iteration 2. */
@Composable
fun GenreScreen(onBack: () -> Unit, viewModel: GenreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter = when (uiState.filter) {
        MediaFilter.ALL -> stringResource(R.string.filter_all)
        MediaFilter.MOVIES -> stringResource(R.string.filter_movies)
        MediaFilter.SERIES -> stringResource(R.string.filter_series)
    }
    KinopolkaScaffold(
        title = uiState.title ?: stringResource(R.string.genre_title),
        onBack = onBack,
    ) { contentModifier ->
        PlaceholderContent(
            text = stringResource(R.string.placeholder_genre, filter),
            modifier = contentModifier,
        )
    }
}

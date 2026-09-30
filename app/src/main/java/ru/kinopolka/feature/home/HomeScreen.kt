package ru.kinopolka.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kinopolka.R
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenGenre: (Genre, MediaFilter) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenSearch = onOpenSearch,
        onOpenGenre = { onOpenGenre(it, MediaFilter.ALL) },
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onRetry: () -> Unit,
) {
    KinopolkaScaffold(title = stringResource(R.string.app_name)) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            SearchEntryPoint(
                onClick = onOpenSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (uiState) {
                HomeUiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }

                is HomeUiState.Error -> PlaceholderContent(text = stringResource(uiState.error.messageRes())) {
                    Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }

                is HomeUiState.Success -> GenreList(
                    genres = uiState.genres,
                    refreshError = uiState.refreshError,
                    onOpenGenre = onOpenGenre,
                )
            }
        }
    }
}

/** The search field on the home screen is only an entry point to the search tab. */
@Composable
private fun SearchEntryPoint(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            leadingContent = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.home_search_hint)) },
        )
    }
}

@Composable
private fun GenreList(genres: List<Genre>, refreshError: DataError?, onOpenGenre: (Genre) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        if (refreshError != null) {
            item {
                Text(
                    text = stringResource(refreshError.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.home_genres_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(genres, key = { it.key }) { genre ->
            ListItem(
                headlineContent = { Text(genre.name) },
                supportingContent = { Text(stringResource(genre.availabilityRes())) },
                modifier = Modifier.clickable(role = Role.Button) { onOpenGenre(genre) },
            )
        }
    }
}

private fun Genre.availabilityRes(): Int = when (mediaTypes) {
    setOf(MediaType.MOVIE) -> R.string.genre_movies_only
    setOf(MediaType.TV) -> R.string.genre_series_only
    else -> R.string.genre_movies_and_series
}

internal fun DataError.messageRes(): Int = when (this) {
    DataError.NO_CONNECTION -> R.string.error_no_connection

    DataError.UNAUTHORIZED -> R.string.error_unauthorized

    DataError.NOT_FOUND,
    DataError.SERVER,
    DataError.BAD_RESPONSE,
    -> R.string.error_generic
}

package ru.kinopolka.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kinopolka.R
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.Shelf
import ru.kinopolka.core.ui.component.EmptyState
import ru.kinopolka.core.ui.component.ErrorState
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.MediaFilterChips
import ru.kinopolka.core.ui.component.MediaShelf
import ru.kinopolka.core.ui.component.OfflineBanner
import ru.kinopolka.core.ui.component.ShelfSkeleton
import ru.kinopolka.core.ui.component.messageRes

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenGenre: (Genre, MediaFilter) -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenSearch = onOpenSearch,
        onFilterChange = viewModel::onFilterChange,
        onOpenGenre = { onOpenGenre(it, uiState.filter) },
        onOpenMedia = onOpenMedia,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onOpenSearch: () -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    onRetry: () -> Unit,
) {
    KinopolkaScaffold(title = stringResource(R.string.app_name)) { contentModifier ->
        // The list state is saveable: the scroll position survives rotation and process death (N-05).
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = contentModifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isOffline) {
                item(key = "offline") { OfflineBanner() }
            }
            item(key = "search") {
                SearchEntryPoint(
                    onClick = onOpenSearch,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item(key = "filter") {
                MediaFilterChips(selected = uiState.filter, onSelect = onFilterChange)
            }
            when (uiState.content) {
                HomeContent.LOADING -> items(SKELETON_SHELVES) { ShelfPlaceholder() }

                HomeContent.ERROR -> item(key = "error") {
                    ErrorState(
                        error = requireNotNull(uiState.error),
                        onRetry = onRetry,
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }

                HomeContent.EMPTY -> item(key = "empty") {
                    EmptyState(
                        message = stringResource(R.string.home_empty),
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }

                HomeContent.DATA -> {
                    val error = uiState.error
                    if (error != null && !uiState.isOffline) {
                        item(key = "refreshError") { RefreshErrorText(error.messageRes()) }
                    }
                    items(uiState.shelves, key = { it.shelf.key }) { content ->
                        // Empty shelves are hidden once loading is over.
                        if (content.items.isNotEmpty() || uiState.isRefreshing) {
                            val genre = (content.shelf as? Shelf.ByGenre)?.genre
                            MediaShelf(
                                title = content.shelf.title(),
                                items = content.items,
                                isLoading = uiState.isRefreshing,
                                onOpenMedia = onOpenMedia,
                                onSeeAll = genre?.let { { onOpenGenre(it) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The search field on the home screen is only an entry point to the search screen. */
@Composable
private fun SearchEntryPoint(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // One node for TalkBack: the card is the button and reads its text.
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null)
            Text(stringResource(R.string.home_search_hint), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ShelfPlaceholder() {
    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShelfSkeleton()
    }
}

@Composable
private fun RefreshErrorText(messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun Shelf.title(): String = when (this) {
    is Shelf.Trending -> stringResource(R.string.home_trending)

    is Shelf.Popular -> when (mediaType) {
        MediaType.MOVIE -> stringResource(R.string.home_popular_movies)
        MediaType.TV -> stringResource(R.string.home_popular_series)
    }

    is Shelf.ByGenre -> genre.name
}

/** Stable key of a shelf in the list. */
private val Shelf.key: String
    get() = when (this) {
        is Shelf.Trending -> "trending"
        is Shelf.Popular -> "popular-${mediaType.key}"
        is Shelf.ByGenre -> "genre-${genre.key}"
    }

private const val SKELETON_SHELVES = 4

package ru.kinopolka.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ru.kinopolka.R
import ru.kinopolka.core.data.DataError
import ru.kinopolka.core.data.toDataError
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.ui.component.EmptyState
import ru.kinopolka.core.ui.component.ErrorState
import ru.kinopolka.core.ui.component.InlineError
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.MediaFilterChips
import ru.kinopolka.core.ui.component.OfflineBanner
import ru.kinopolka.core.ui.component.PosterImage
import ru.kinopolka.core.ui.component.SkeletonBox
import ru.kinopolka.core.ui.component.label

/**
 * @param requestFocus `true` when the screen is opened from the search field of the home
 *   screen: the input gets focus and the keyboard opens.
 */
@Composable
fun SearchScreen(
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    SearchScreen(
        uiState = uiState,
        results = results,
        requestFocus = requestFocus,
        onFocusRequested = onFocusRequested,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onOpenMedia = onOpenMedia,
    )
}

@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    results: LazyPagingItems<Media>,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
) {
    KinopolkaScaffold(title = stringResource(R.string.tab_search)) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            if (uiState.isOffline) OfflineBanner()
            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                requestFocus = requestFocus,
                onFocusRequested = onFocusRequested,
            )
            MediaFilterChips(
                selected = uiState.filter,
                onSelect = onFilterChange,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SearchResults(uiState = uiState, results = results, onOpenMedia = onOpenMedia)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun SearchResults(uiState: SearchUiState, results: LazyPagingItems<Media>, onOpenMedia: (MediaKey) -> Unit) {
    if (uiState.isQueryTooShort) {
        EmptyState(message = stringResource(R.string.search_min_length))
        return
    }
    val refresh = results.loadState.refresh
    when {
        uiState.isPending || (refresh is LoadState.Loading && results.itemCount == 0) -> SearchSkeleton()

        refresh is LoadState.Error && results.itemCount == 0 -> {
            val error = refresh.error.toDataError()
            if (error == DataError.NO_CONNECTION) {
                // N-02: search needs the network, cached data cannot help here.
                EmptyState(
                    message = stringResource(R.string.search_offline),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = results::retry,
                )
            } else {
                ErrorState(error = error, onRetry = results::retry)
            }
        }

        refresh is LoadState.NotLoading && results.itemCount == 0 -> EmptyState(stringResource(R.string.search_empty))

        else -> {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    count = results.itemCount,
                    key = results.itemKey { "${it.mediaType.key}-${it.tmdbId}" },
                ) { index ->
                    results[index]?.let { media ->
                        SearchResultRow(
                            media = media,
                            genreNames = uiState.genreNames[media.mediaType].orEmpty(),
                            onClick = { onOpenMedia(media.key) },
                        )
                    }
                }
                appendState(results)
            }
        }
    }
}

private fun LazyListScope.appendState(results: LazyPagingItems<Media>) {
    when (val append = results.loadState.append) {
        is LoadState.Loading -> item(key = "appendLoading") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is LoadState.Error -> item(key = "appendError") {
            InlineError(error = append.error.toDataError(), onRetry = results::retry)
        }

        is LoadState.NotLoading -> Unit
    }
}

/** Result row: poster, title, year, type and 1–2 genres (docs/PLAN.md, section 7). */
@Composable
private fun SearchResultRow(media: Media, genreNames: Map<Int, String>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(posterPath = media.posterPath, title = media.title, modifier = Modifier.width(56.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(media.releaseYear?.toString(), media.mediaType.label()).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val genres = media.genreIds.mapNotNull { genreNames[it] }.take(MAX_ROW_GENRES)
            if (genres.isNotEmpty()) {
                Text(
                    text = genres.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchSkeleton() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(SKELETON_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(Modifier.width(56.dp).height(84.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(Modifier.width(180.dp).height(16.dp))
                    SkeletonBox(Modifier.width(120.dp).height(12.dp))
                }
            }
        }
    }
}

private const val MAX_ROW_GENRES = 2
private const val SKELETON_ROWS = 6

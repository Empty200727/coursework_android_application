package ru.kinopolka.feature.genre

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ru.kinopolka.R
import ru.kinopolka.core.data.toDataError
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaSort
import ru.kinopolka.core.ui.component.EmptyState
import ru.kinopolka.core.ui.component.ErrorState
import ru.kinopolka.core.ui.component.InlineError
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.OfflineBanner
import ru.kinopolka.core.ui.component.PosterCard
import ru.kinopolka.core.ui.component.PosterSkeleton
import ru.kinopolka.core.ui.component.labelRes

@Composable
fun GenreScreen(onBack: () -> Unit, onOpenMedia: (MediaKey) -> Unit, viewModel: GenreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val media = viewModel.media.collectAsLazyPagingItems()
    GenreScreen(
        uiState = uiState,
        media = media,
        onBack = onBack,
        onSortChange = viewModel::onSortChange,
        onOpenMedia = onOpenMedia,
    )
}

@Composable
internal fun GenreScreen(
    uiState: GenreUiState,
    media: LazyPagingItems<Media>,
    onBack: () -> Unit,
    onSortChange: (MediaSort) -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
) {
    val title = uiState.title ?: stringResource(R.string.genre_title)
    val fullTitle = if (uiState.filter == MediaFilter.ALL) {
        title
    } else {
        stringResource(R.string.genre_title_with_filter, title, stringResource(uiState.filter.labelRes()))
    }
    KinopolkaScaffold(title = fullTitle, onBack = onBack) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            if (uiState.isOffline) OfflineBanner()
            SortChips(selected = uiState.sort, onSelect = onSortChange)
            GenreGrid(media = media, onOpenMedia = onOpenMedia)
        }
    }
}

@Composable
private fun SortChips(selected: MediaSort, onSelect: (MediaSort) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.labelRes())) },
            )
        }
    }
}

@Composable
private fun GenreGrid(media: LazyPagingItems<Media>, onOpenMedia: (MediaKey) -> Unit) {
    val refresh = media.loadState.refresh
    when {
        refresh is LoadState.Error && media.itemCount == 0 ->
            ErrorState(error = refresh.error.toDataError(), onRetry = media::retry)

        refresh is LoadState.NotLoading && media.itemCount == 0 && media.loadState.append.endOfPaginationReached ->
            EmptyState(message = stringResource(R.string.genre_empty))

        else -> {
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GRID_MIN_CELL),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Errors and the empty result are handled above: no items yet means loading.
                if (media.itemCount == 0) {
                    items(SKELETON_ITEMS) { PosterSkeleton() }
                } else {
                    items(
                        count = media.itemCount,
                        key = media.itemKey { "${it.mediaType.key}-${it.tmdbId}" },
                    ) { index ->
                        media[index]?.let { item ->
                            PosterCard(media = item, onClick = { onOpenMedia(item.key) })
                        }
                    }
                    appendState(media)
                }
            }
        }
    }
}

private fun LazyGridScope.appendState(media: LazyPagingItems<Media>) {
    val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan =
        { GridItemSpan(maxLineSpan) }
    when (val append = media.loadState.append) {
        is LoadState.Loading -> item(key = "appendLoading", span = fullWidth) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is LoadState.Error -> item(key = "appendError", span = fullWidth) {
            InlineError(error = append.error.toDataError(), onRetry = media::retry)
        }

        is LoadState.NotLoading -> Unit
    }
}

private fun MediaSort.labelRes(): Int = when (this) {
    MediaSort.POPULARITY -> R.string.sort_popular
    MediaSort.RATING -> R.string.sort_rating
    MediaSort.NEWEST -> R.string.sort_newest
}

private val GRID_MIN_CELL = 110.dp
private const val SKELETON_ITEMS = 9

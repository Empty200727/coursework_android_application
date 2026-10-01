package ru.kinopolka.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kinopolka.R
import ru.kinopolka.core.model.LibraryItem
import ru.kinopolka.core.model.LibrarySort
import ru.kinopolka.core.model.LibraryTab
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.ui.component.EmptyState
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.MediaFilterChips
import ru.kinopolka.core.ui.component.PosterImage
import ru.kinopolka.core.ui.component.label
import ru.kinopolka.core.ui.component.subtitle

@Composable
fun LibraryScreen(
    onOpenAbout: () -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    onFindSomething: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onOpenAbout = onOpenAbout,
        onOpenMedia = onOpenMedia,
        onFindSomething = onFindSomething,
        onTabChange = viewModel::onTabChange,
        onSortChange = viewModel::onSortChange,
        onFilterChange = viewModel::onFilterChange,
        onRemove = viewModel::onRemove,
        onUndo = viewModel::onUndo,
        onUndoDismissed = viewModel::onUndoDismissed,
    )
}

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onOpenAbout: () -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    onFindSomething: () -> Unit,
    onTabChange: (LibraryTab) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onRemove: (LibraryItem) -> Unit,
    onUndo: () -> Unit,
    onUndoDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UndoSnackbar(uiState.pendingUndo, snackbarHostState, onUndo, onUndoDismissed)
    KinopolkaScaffold(
        title = stringResource(R.string.tab_library),
        snackbarHostState = snackbarHostState,
        actions = {
            SortMenu(selected = uiState.sort, onSelect = onSortChange)
            IconButton(onClick = onOpenAbout) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = stringResource(R.string.about_title))
            }
        },
    ) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = uiState.tab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == uiState.tab,
                        onClick = { onTabChange(tab) },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.library_tab_count,
                                    stringResource(tab.labelRes()),
                                    uiState.counts[tab] ?: 0,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
            MediaFilterChips(
                selected = uiState.filter,
                onSelect = onFilterChange,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when {
                uiState.isLoading -> Unit

                uiState.items.isEmpty() -> EmptyState(
                    message = stringResource(uiState.tab.emptyRes()),
                    actionLabel = stringResource(R.string.library_find_something),
                    onAction = onFindSomething,
                )

                else -> LibraryList(uiState, onOpenMedia, onRemove)
            }
        }
    }
}

@Composable
private fun LibraryList(uiState: LibraryUiState, onOpenMedia: (MediaKey) -> Unit, onRemove: (LibraryItem) -> Unit) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(uiState.items, key = { "${it.media.mediaType.key}-${it.media.tmdbId}" }) { item ->
            SwipeToRemove(onRemove = { onRemove(item) }) {
                LibraryRow(
                    item = item,
                    tab = uiState.tab,
                    onClick = { onOpenMedia(item.media.key) },
                    onRemove = { onRemove(item) },
                )
            }
        }
    }
}

/** F-13: a swipe in either direction removes the title; the row button does the same. */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onRemove()
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, tab: LibraryTab, onClick: () -> Unit, onRemove: () -> Unit) {
    val media = item.media
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            posterPath = media.posterPath,
            title = media.title,
            localPath = item.entry.localPosterPath,
            modifier = Modifier.width(56.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                media.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(media.mediaType.label(), media.subtitle()).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.library_remove, stringResource(tab.labelRes())),
            )
        }
    }
}

@Composable
private fun SortMenu(selected: LibrarySort, onSelect: (LibrarySort) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.library_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes())) },
                    leadingIcon = { RadioButton(selected = sort == selected, onClick = null) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun UndoSnackbar(
    pendingUndo: PendingUndo?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onUndoDismissed: () -> Unit,
) {
    val message = pendingUndo?.let {
        stringResource(R.string.library_removed, it.title, stringResource(it.tab.labelRes()))
    }
    val action = stringResource(R.string.action_undo)
    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null || message == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo() else onUndoDismissed()
    }
}

private fun LibraryTab.labelRes(): Int = when (this) {
    LibraryTab.WANT -> R.string.library_tab_want
    LibraryTab.WATCHED -> R.string.library_tab_watched
    LibraryTab.FAVORITES -> R.string.library_tab_favorites
}

private fun LibraryTab.emptyRes(): Int = when (this) {
    LibraryTab.WANT -> R.string.library_empty_want
    LibraryTab.WATCHED -> R.string.library_empty_watched
    LibraryTab.FAVORITES -> R.string.library_empty_favorites
}

private fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.ADDED -> R.string.library_sort_added
    LibrarySort.TITLE -> R.string.library_sort_title
    LibrarySort.RATING -> R.string.library_sort_rating
}

package ru.kinopolka.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.kinopolka.R
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaKey

/**
 * Horizontal shelf (F-05, F-07): title, optional «Все» button and up to 20 posters.
 * Shows skeletons while [isLoading] and nothing is cached yet.
 */
@Composable
fun MediaShelf(
    title: String,
    items: List<Media>,
    isLoading: Boolean,
    onOpenMedia: (MediaKey) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (onSeeAll != null) {
                val description = stringResource(R.string.shelf_see_all_description, title)
                TextButton(
                    onClick = onSeeAll,
                    modifier = Modifier.semantics { contentDescription = description },
                ) {
                    Text(stringResource(R.string.shelf_see_all))
                }
            }
        }
        if (items.isEmpty() && isLoading) {
            ShelfSkeleton()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { "${it.mediaType.key}-${it.tmdbId}" }) { media ->
                    ShelfPosterCard(media = media, onClick = { onOpenMedia(media.key) })
                }
            }
        }
    }
}

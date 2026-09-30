package ru.kinopolka.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kinopolka.R
import ru.kinopolka.core.model.MediaFilter

/** «Все / Фильмы / Сериалы» (F-02), shared by the home and search screens. */
@Composable
fun MediaFilterChips(selected: MediaFilter, onSelect: (MediaFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes())) },
            )
        }
    }
}

fun MediaFilter.labelRes(): Int = when (this) {
    MediaFilter.ALL -> R.string.filter_all
    MediaFilter.MOVIES -> R.string.filter_movies
    MediaFilter.SERIES -> R.string.filter_series
}

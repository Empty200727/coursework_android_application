package ru.kinopolka.feature.library

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ru.kinopolka.R
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent

/** «Моя полка» (F-12, F-13) is implemented in iteration 3. */
@Composable
fun LibraryScreen(onOpenAbout: () -> Unit) {
    KinopolkaScaffold(
        title = stringResource(R.string.tab_library),
        actions = {
            IconButton(onClick = onOpenAbout) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.about_title),
                )
            }
        },
    ) { contentModifier ->
        PlaceholderContent(text = stringResource(R.string.placeholder_library), modifier = contentModifier)
    }
}

package ru.kinopolka.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kinopolka.R
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent

/** Search (F-01…F-03) is implemented in iteration 2. */
@Composable
fun SearchScreen() {
    KinopolkaScaffold(title = stringResource(R.string.tab_search)) { contentModifier ->
        PlaceholderContent(text = stringResource(R.string.placeholder_search), modifier = contentModifier)
    }
}

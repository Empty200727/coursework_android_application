package ru.kinopolka.feature.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ru.kinopolka.BuildConfig
import ru.kinopolka.R
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent

/** «О приложении» (F-14): the TMDB logo and link are added in iteration 3. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    KinopolkaScaffold(title = stringResource(R.string.about_title), onBack = onBack) { contentModifier ->
        PlaceholderContent(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            modifier = contentModifier,
        ) {
            Text(
                text = stringResource(R.string.about_tmdb_notice),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

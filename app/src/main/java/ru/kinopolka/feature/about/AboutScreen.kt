package ru.kinopolka.feature.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kinopolka.BuildConfig
import ru.kinopolka.R
import ru.kinopolka.core.ui.component.KinopolkaScaffold

private const val TMDB_URL = "https://www.themoviedb.org"

/**
 * «О приложении» (F-14): version and the TMDB attribution — the approved logo, smaller than the
 * logo of the app, and the required notice (docs/PLAN.md, section 4).
 */
@Composable
fun AboutScreen(onBack: () -> Unit, versionName: String = BuildConfig.VERSION_NAME) {
    val uriHandler = LocalUriHandler.current
    KinopolkaScaffold(title = stringResource(R.string.about_title), onBack = onBack) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(APP_LOGO_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(text = stringResource(R.string.about_data_source), style = MaterialTheme.typography.titleSmall)
            Image(
                painter = painterResource(R.drawable.tmdb_logo),
                contentDescription = stringResource(R.string.about_tmdb_logo),
                modifier = Modifier
                    .fillMaxWidth(TMDB_LOGO_WIDTH_FRACTION)
                    .height(TMDB_LOGO_HEIGHT),
            )
            Text(
                text = stringResource(R.string.about_tmdb_notice),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = { uriHandler.openUri(TMDB_URL) }) {
                Icon(painter = painterResource(R.drawable.ic_open_in_new), contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.about_tmdb_link))
            }
        }
    }
}

private val APP_LOGO_SIZE = 96.dp
private val TMDB_LOGO_HEIGHT = 16.dp
private const val TMDB_LOGO_WIDTH_FRACTION = 0.5f

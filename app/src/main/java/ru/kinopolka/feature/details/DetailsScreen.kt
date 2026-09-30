package ru.kinopolka.feature.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kinopolka.R
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.PlaceholderContent
import ru.kinopolka.navigation.DetailsRoute

/** Title card (F-08…F-11) is implemented in iteration 3; the stub shows the route arguments. */
@Composable
fun DetailsScreen(route: DetailsRoute, onBack: () -> Unit) {
    val type = when (route.mediaType) {
        MediaType.MOVIE -> stringResource(R.string.media_type_movie)
        MediaType.TV -> stringResource(R.string.media_type_tv)
    }
    KinopolkaScaffold(title = stringResource(R.string.details_title), onBack = onBack) { contentModifier ->
        PlaceholderContent(
            text = stringResource(R.string.placeholder_details, type, route.id),
            modifier = contentModifier,
        )
    }
}

package ru.kinopolka.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.util.Locale
import ru.kinopolka.BuildConfig
import ru.kinopolka.R
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.ImageSize
import ru.kinopolka.core.network.tmdbImageUrl

/** Width of a poster in horizontal shelves. */
val ShelfPosterWidth: Dp = 120.dp

private const val POSTER_ASPECT_RATIO = 2f / 3f

/**
 * Poster with the title under it (N-04: `w342`). Without an image the title is drawn on
 * the placeholder instead (docs/PLAN.md, section 7).
 */
@Composable
fun PosterCard(media: Media, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            PosterImage(
                posterPath = media.posterPath,
                title = media.title,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = media.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = media.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/** 2:3 poster; the placeholder with the title stays visible while the image loads or if it fails. */
@Composable
fun PosterImage(
    posterPath: String?,
    title: String,
    modifier: Modifier = Modifier,
    size: ImageSize = ImageSize.POSTER_LIST,
) {
    val url = tmdbImageUrl(posterPath, size, BuildConfig.TMDB_IMAGE_BASE_URL)
    Box(
        modifier = modifier
            .aspectRatio(POSTER_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            // The title is already read from the text next to the poster.
            modifier = Modifier
                .padding(8.dp)
                .clearAndSetSemantics {},
        )
        if (url != null) {
            AsyncImage(
                model = url,
                // The title is shown next to the poster, so the image itself is decorative.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** «2008 · ★ 8.9»: year and rating when TMDB has them. */
@Composable
fun Media.subtitle(): String {
    val rating = voteAverage.takeIf { voteCount > 0 }?.let { String.format(Locale.ROOT, "%.1f", it) }
    return listOfNotNull(releaseYear?.toString(), rating?.let { stringResource(R.string.rating_short, it) })
        .joinToString(" · ")
}

@Composable
fun MediaType.label(): String = when (this) {
    MediaType.MOVIE -> stringResource(R.string.media_type_movie)
    MediaType.TV -> stringResource(R.string.media_type_tv)
}

/** Poster card of the width used in shelves. */
@Composable
fun ShelfPosterCard(media: Media, onClick: () -> Unit) {
    PosterCard(media = media, onClick = onClick, modifier = Modifier.width(ShelfPosterWidth))
}

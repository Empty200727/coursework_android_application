package ru.kinopolka.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SKELETON_MIN_ALPHA = 0.35f
private const val POSTER_RATIO = 2f / 3f
private const val TITLE_LINE_FRACTION = 0.8f
private const val SUBTITLE_LINE_FRACTION = 0.5f
private const val SKELETON_PULSE_MILLIS = 900

/** Pulsing placeholder block shown while content loads (docs/PLAN.md, section 7). */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = SKELETON_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(SKELETON_PULSE_MILLIS), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** Skeleton of a poster card: image and two text lines. */
@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(POSTER_RATIO))
        SkeletonBox(Modifier.fillMaxWidth(TITLE_LINE_FRACTION).height(14.dp))
        SkeletonBox(Modifier.fillMaxWidth(SUBTITLE_LINE_FRACTION).height(12.dp))
    }
}

/** A row of poster skeletons in place of a shelf. */
@Composable
fun ShelfSkeleton(
    modifier: Modifier = Modifier,
    posterWidth: Dp = ShelfPosterWidth,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Row(
        modifier = modifier.padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(SKELETON_POSTERS) { PosterSkeleton(Modifier.width(posterWidth)) }
    }
}

private const val SKELETON_POSTERS = 4

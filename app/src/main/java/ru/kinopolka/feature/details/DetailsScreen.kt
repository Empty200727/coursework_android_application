package ru.kinopolka.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.util.Locale
import ru.kinopolka.BuildConfig
import ru.kinopolka.R
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus
import ru.kinopolka.core.network.ImageSize
import ru.kinopolka.core.network.tmdbImageUrl
import ru.kinopolka.core.ui.component.ErrorState
import ru.kinopolka.core.ui.component.KinopolkaScaffold
import ru.kinopolka.core.ui.component.MediaShelf
import ru.kinopolka.core.ui.component.OfflineBanner
import ru.kinopolka.core.ui.component.PosterImage
import ru.kinopolka.core.ui.component.ShelfSkeleton
import ru.kinopolka.core.ui.component.SkeletonBox
import ru.kinopolka.core.ui.component.label

@Composable
fun DetailsScreen(onBack: () -> Unit, onOpenMedia: (MediaKey) -> Unit, viewModel: DetailsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenMedia = onOpenMedia,
        onStatusClick = viewModel::onStatusClick,
        onFavoriteClick = viewModel::onFavoriteClick,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun DetailsScreen(
    uiState: DetailsUiState,
    onBack: () -> Unit,
    onOpenMedia: (MediaKey) -> Unit,
    onStatusClick: (WatchStatus) -> Unit,
    onFavoriteClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val title = uiState.details?.media?.title ?: stringResource(R.string.details_title)
    KinopolkaScaffold(title = title, onBack = onBack) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            if (uiState.isOffline) OfflineBanner()
            when (uiState.content) {
                DetailsContent.LOADING -> DetailsSkeleton()

                DetailsContent.ERROR -> ErrorState(
                    error = requireNotNull(uiState.error),
                    onRetry = onRetry,
                )

                DetailsContent.DATA -> DetailsContent(
                    uiState = uiState,
                    details = requireNotNull(uiState.details),
                    onOpenMedia = onOpenMedia,
                    onStatusClick = onStatusClick,
                    onFavoriteClick = onFavoriteClick,
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(
    uiState: DetailsUiState,
    details: MediaDetails,
    onOpenMedia: (MediaKey) -> Unit,
    onStatusClick: (WatchStatus) -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val loadingExtras = uiState.isRefreshing && !details.isComplete
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Header(details, localPosterPath = uiState.entry?.localPosterPath) }
        item(key = "actions") {
            LibraryActions(
                status = uiState.watchStatus,
                isFavorite = uiState.isFavorite,
                onStatusClick = onStatusClick,
                onFavoriteClick = onFavoriteClick,
            )
        }
        item(key = "overview") { Overview(details, isLoading = loadingExtras) }
        if (details.cast.isNotEmpty() || loadingExtras) {
            item(key = "cast") { Cast(details.cast, isLoading = loadingExtras) }
        }
        if (details.related.isNotEmpty() || loadingExtras) {
            item(key = "related") {
                MediaShelf(
                    title = stringResource(R.string.details_related),
                    items = details.related,
                    isLoading = loadingExtras,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}

@Composable
private fun Header(details: MediaDetails, localPosterPath: String?) {
    val media = details.media
    Column {
        Box {
            Backdrop(media.backdropPath)
            PosterImage(
                posterPath = media.posterPath,
                title = media.title,
                size = ImageSize.POSTER_DETAILS,
                localPath = localPosterPath,
                modifier = Modifier
                    .padding(start = 16.dp, top = BACKDROP_POSTER_OFFSET)
                    .width(POSTER_WIDTH),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            media.originalTitle?.takeIf { it != media.title }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(metaLine(details), style = MaterialTheme.typography.bodyMedium)
            ratingLine(details)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (details.genres.isNotEmpty()) {
                Text(
                    text = details.genres.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Backdrop with a fade to the background; the poster overlaps its lower part. */
@Composable
private fun Backdrop(backdropPath: String?) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(BACKDROP_ASPECT_RATIO)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val url = tmdbImageUrl(backdropPath, ImageSize.BACKDROP, BuildConfig.TMDB_IMAGE_BASE_URL)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
        )
    }
}

/** «2008–2013 · Сериал · 5 сезонов» or «1999 · Фильм · 2 ч 19 мин». */
@Composable
private fun metaLine(details: MediaDetails): String {
    val parts = mutableListOf<String>()
    details.yearsText(ongoing = stringResource(R.string.details_ongoing))?.let(parts::add)
    parts += details.media.mediaType.label()
    when (details.media.mediaType) {
        MediaType.MOVIE -> details.runtimeMinutes?.let { minutes ->
            val (hours, rest) = splitRuntime(minutes)
            parts += if (hours > 0) {
                stringResource(R.string.details_runtime, hours, rest)
            } else {
                stringResource(R.string.details_runtime_minutes, rest)
            }
        }

        MediaType.TV -> details.numberOfSeasons?.let {
            parts += pluralStringResource(R.plurals.details_seasons, it, it)
        }
    }
    return parts.joinToString(" · ")
}

/** «★ 8.4 · 29 870 оценок». */
@Composable
private fun ratingLine(details: MediaDetails): String? {
    val media = details.media
    if (media.voteCount <= 0) return null
    val rating = String.format(Locale.ROOT, "%.1f", media.voteAverage)
    val votes = pluralStringResource(R.plurals.details_votes, media.voteCount, formatCount(media.voteCount))
    return stringResource(R.string.rating_short, rating) + " · " + votes
}

/** F-11: «Хочу посмотреть» / «Смотрел» exclude each other, «В избранное» is separate. */
@Composable
private fun LibraryActions(
    status: WatchStatus,
    isFavorite: Boolean,
    onStatusClick: (WatchStatus) -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf(
            WatchStatus.WANT to stringResource(R.string.details_status_want),
            WatchStatus.WATCHED to stringResource(R.string.details_status_watched),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = status == option,
                    onClick = { onStatusClick(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        IconToggleButton(checked = isFavorite, onCheckedChange = { onFavoriteClick() }) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = stringResource(
                    if (isFavorite) R.string.details_favorite_remove else R.string.details_favorite_add,
                ),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** Overview collapsed to a few lines with «Ещё» (docs/PLAN.md, section 7). */
@Composable
private fun Overview(details: MediaDetails, isLoading: Boolean) {
    val overview = details.media.overview
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            overview != null -> {
                var expanded by rememberSaveable { mutableStateOf(false) }
                var overflows by rememberSaveable { mutableStateOf(false) }
                if (details.isOverviewFallback) {
                    Text(
                        text = stringResource(R.string.details_overview_english),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_OVERVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                )
                if (overflows || expanded) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(
                            stringResource(
                                if (expanded) R.string.details_overview_less else R.string.details_overview_more,
                            ),
                        )
                    }
                }
            }

            isLoading -> repeat(SKELETON_OVERVIEW_LINES) { SkeletonBox(Modifier.fillMaxWidth().height(16.dp)) }

            else -> Text(
                text = stringResource(R.string.details_no_overview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** F-09: first 10 actors with photo, name and role. */
@Composable
private fun Cast(cast: List<CastMember>, isLoading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.details_cast),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { heading() },
        )
        if (cast.isEmpty() && isLoading) {
            ShelfSkeleton(posterWidth = CAST_PHOTO_SIZE)
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cast, key = { it.personId }) { member -> CastCard(member) }
            }
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    Column(
        modifier = Modifier.width(CAST_CARD_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CAST_PHOTO_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val url = tmdbImageUrl(member.profilePath, ImageSize.PROFILE, BuildConfig.TMDB_IMAGE_BASE_URL)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(member.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        member.character?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(BACKDROP_ASPECT_RATIO))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(SKELETON_TITLE_FRACTION).height(28.dp))
            SkeletonBox(Modifier.fillMaxWidth(SKELETON_META_FRACTION).height(16.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(40.dp))
        }
        ShelfSkeleton()
    }
}

private const val BACKDROP_ASPECT_RATIO = 16f / 9f
private const val COLLAPSED_OVERVIEW_LINES = 4
private const val SKELETON_OVERVIEW_LINES = 3
private const val SKELETON_TITLE_FRACTION = 0.7f
private const val SKELETON_META_FRACTION = 0.5f
private val BACKDROP_POSTER_OFFSET = 120.dp
private val POSTER_WIDTH = 110.dp
private val CAST_PHOTO_SIZE = 72.dp
private val CAST_CARD_WIDTH = 88.dp

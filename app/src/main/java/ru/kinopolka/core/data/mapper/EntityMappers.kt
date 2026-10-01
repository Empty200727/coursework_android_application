package ru.kinopolka.core.data.mapper

import java.time.Instant
import ru.kinopolka.core.database.entity.CastMemberEntity
import ru.kinopolka.core.database.entity.FeedItemEntity
import ru.kinopolka.core.database.entity.GenreEntity
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.database.entity.MediaEntity
import ru.kinopolka.core.database.entity.MediaGenreEntity
import ru.kinopolka.core.database.entity.RelatedMediaEntity
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.LibraryEntry
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.RelatedKind
import ru.kinopolka.core.model.TmdbGenre

/** A list item: detail columns stay empty until the title card is opened. */
fun Media.toEntity(cachedAt: Long): MediaEntity = MediaEntity(
    mediaType = key.mediaType,
    tmdbId = key.tmdbId,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    isOverviewFallback = false,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.toString(),
    lastAirDate = null,
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    runtime = null,
    numberOfSeasons = null,
    inProduction = null,
    cachedAt = cachedAt,
    detailsCachedAt = null,
)

fun MediaDetails.toEntity(cachedAt: Long): MediaEntity = media.toEntity(cachedAt).copy(
    isOverviewFallback = isOverviewFallback,
    lastAirDate = lastAirDate?.toString(),
    runtime = runtimeMinutes,
    numberOfSeasons = numberOfSeasons,
    inProduction = inProduction,
    detailsCachedAt = cachedAt,
)

fun MediaEntity.toMedia(genreIds: List<Int> = emptyList()): Media = Media(
    key = MediaKey(mediaType, tmdbId),
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate.toLocalDateOrNull(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    genreIds = genreIds,
)

fun Media.toGenreEntities(): List<MediaGenreEntity> =
    genreIds.distinct().map { MediaGenreEntity(key.mediaType, key.tmdbId, it) }

fun TmdbGenre.toEntity(cachedAt: Long): GenreEntity = GenreEntity(
    mediaType = mediaType,
    genreId = id,
    name = name,
    cachedAt = cachedAt,
)

fun GenreEntity.toTmdbGenre(): TmdbGenre = TmdbGenre(mediaType = mediaType, id = genreId, name = name)

fun CastMember.toEntity(key: MediaKey): CastMemberEntity = CastMemberEntity(
    mediaType = key.mediaType,
    tmdbId = key.tmdbId,
    personId = personId,
    name = name,
    character = character,
    profilePath = profilePath,
    castOrder = order,
)

fun CastMemberEntity.toCastMember(): CastMember = CastMember(
    personId = personId,
    name = name,
    character = character,
    profilePath = profilePath,
    order = castOrder,
)

fun relatedEntities(source: MediaKey, kind: RelatedKind, targets: List<Media>): List<RelatedMediaEntity> =
    targets.distinctBy { it.key }.mapIndexed { position, target ->
        RelatedMediaEntity(
            sourceMediaType = source.mediaType,
            sourceTmdbId = source.tmdbId,
            kind = kind,
            targetMediaType = target.key.mediaType,
            targetTmdbId = target.key.tmdbId,
            position = position,
        )
    }

fun feedItemEntities(feedKey: String, items: List<Media>, fetchedAt: Long): List<FeedItemEntity> =
    items.distinctBy { it.key }.mapIndexed { position, media ->
        FeedItemEntity(
            feedKey = feedKey,
            position = position,
            mediaType = media.key.mediaType,
            tmdbId = media.key.tmdbId,
            fetchedAt = fetchedAt,
        )
    }

fun LibraryEntry.toEntity(): LibraryEntryEntity = LibraryEntryEntity(
    mediaType = key.mediaType,
    tmdbId = key.tmdbId,
    watchStatus = watchStatus,
    isFavorite = isFavorite,
    addedAt = addedAt.toEpochMilli(),
    watchedAt = watchedAt?.toEpochMilli(),
    userRating = userRating,
    localPosterPath = localPosterPath,
)

fun LibraryEntryEntity.toLibraryEntry(): LibraryEntry = LibraryEntry(
    key = MediaKey(mediaType, tmdbId),
    watchStatus = watchStatus,
    isFavorite = isFavorite,
    addedAt = Instant.ofEpochMilli(addedAt),
    watchedAt = watchedAt?.let(Instant::ofEpochMilli),
    userRating = userRating,
    localPosterPath = localPosterPath,
)

/** Title card from the cache; works offline with whatever was stored (N-01). */
fun MediaEntity.toMediaDetails(
    genreIds: List<Int>,
    genreNames: Map<Int, String>,
    cast: List<CastMemberEntity>,
    recommendations: List<MediaEntity>,
    similar: List<MediaEntity>,
): MediaDetails = MediaDetails(
    media = toMedia(genreIds),
    genres = genreIds.mapNotNull { id -> genreNames[id]?.let { TmdbGenre(mediaType, id, it) } },
    runtimeMinutes = runtime.takeIf { mediaType == MediaType.MOVIE },
    numberOfSeasons = numberOfSeasons.takeIf { mediaType == MediaType.TV },
    lastAirDate = lastAirDate.toLocalDateOrNull(),
    inProduction = inProduction,
    isOverviewFallback = isOverviewFallback,
    cast = cast.map { it.toCastMember() },
    recommendations = recommendations.map { it.toMedia() },
    similar = similar.map { it.toMedia() },
    isComplete = detailsCachedAt != null,
)

package ru.kinopolka.core.data.mapper

import java.time.LocalDate
import ru.kinopolka.core.model.CastMember
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.TmdbGenre
import ru.kinopolka.core.network.model.AggregateCastDto
import ru.kinopolka.core.network.model.CastDto
import ru.kinopolka.core.network.model.GenreDto
import ru.kinopolka.core.network.model.GenreListResponseDto
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.MovieDetailsDto
import ru.kinopolka.core.network.model.PagedResponseDto
import ru.kinopolka.core.network.model.TvDetailsDto

/** F-09: the title card shows the first 10 actors. */
const val MAX_CAST_MEMBERS = 10

/** TMDB dates are `yyyy-MM-dd`; an empty or malformed date means «unknown». */
fun String?.toLocalDateOrNull(): LocalDate? {
    if (isNullOrBlank()) return null
    return runCatching { LocalDate.parse(trim()) }.getOrNull()
}

private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

/**
 * Maps a list item to [Media]. The type comes from `media_type` when TMDB sends it
 * (search/multi, trending) and from [defaultType] otherwise (popular, discover).
 * Returns `null` for people (F-01), unknown types and items without id or title.
 */
fun MediaListItemDto.toMediaOrNull(defaultType: MediaType? = null): Media? {
    val type = if (mediaType != null) MediaType.fromKey(mediaType) else defaultType
    val id = id
    if (type == null || id == null) return null
    val names = namesFor(type)
    val resolvedTitle = names.title ?: names.originalTitle ?: return null
    return Media(
        key = MediaKey(type, id),
        title = resolvedTitle,
        originalTitle = names.originalTitle,
        overview = overview.nullIfBlank(),
        posterPath = posterPath.nullIfBlank(),
        backdropPath = backdropPath.nullIfBlank(),
        releaseDate = names.date.toLocalDateOrNull(),
        voteAverage = voteAverage ?: 0.0,
        voteCount = voteCount ?: 0,
        popularity = popularity ?: 0.0,
        genreIds = genreIds.orEmpty(),
    )
}

private class ItemNames(val title: String?, val originalTitle: String?, val date: String?)

/** Movies use `title` and `release_date`, series `name` and `first_air_date`. */
private fun MediaListItemDto.namesFor(type: MediaType): ItemNames = when (type) {
    MediaType.MOVIE -> ItemNames(
        title = (title ?: name).nullIfBlank(),
        originalTitle = (originalTitle ?: originalName).nullIfBlank(),
        date = releaseDate ?: firstAirDate,
    )

    MediaType.TV -> ItemNames(
        title = (name ?: title).nullIfBlank(),
        originalTitle = (originalName ?: originalTitle).nullIfBlank(),
        date = firstAirDate ?: releaseDate,
    )
}

/** Maps a page of results, dropping people and duplicates. */
fun PagedResponseDto<MediaListItemDto>?.toMediaList(defaultType: MediaType? = null): List<Media> =
    this?.results.orEmpty()
        .mapNotNull { it.toMediaOrNull(defaultType) }
        .distinctBy { it.key }

fun GenreDto.toTmdbGenreOrNull(type: MediaType): TmdbGenre? {
    val id = id ?: return null
    val name = name.nullIfBlank() ?: return null
    return TmdbGenre(mediaType = type, id = id, name = name.trim())
}

fun GenreListResponseDto.toTmdbGenres(type: MediaType): List<TmdbGenre> =
    genres.orEmpty().mapNotNull { it.toTmdbGenreOrNull(type) }.distinctBy { it.id }

fun MovieDetailsDto.toMediaDetails(): MediaDetails? {
    val id = id ?: return null
    val genres = genres.orEmpty().mapNotNull { it.toTmdbGenreOrNull(MediaType.MOVIE) }
    val media = MediaListItemDto(
        id = id,
        mediaType = MediaType.MOVIE.key,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = genres.map { it.id },
    ).toMediaOrNull() ?: return null
    return MediaDetails(
        media = media,
        genres = genres,
        runtimeMinutes = runtime?.takeIf { it > 0 },
        numberOfSeasons = null,
        lastAirDate = null,
        inProduction = null,
        isOverviewFallback = false,
        cast = credits?.cast.orEmpty().toCastMembers(),
        recommendations = recommendations.toMediaList(MediaType.MOVIE),
        similar = similar.toMediaList(MediaType.MOVIE),
    )
}

fun TvDetailsDto.toMediaDetails(): MediaDetails? {
    val id = id ?: return null
    val genres = genres.orEmpty().mapNotNull { it.toTmdbGenreOrNull(MediaType.TV) }
    val media = MediaListItemDto(
        id = id,
        mediaType = MediaType.TV.key,
        name = name,
        originalName = originalName,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = genres.map { it.id },
    ).toMediaOrNull() ?: return null
    return MediaDetails(
        media = media,
        genres = genres,
        runtimeMinutes = null,
        numberOfSeasons = numberOfSeasons?.takeIf { it > 0 },
        lastAirDate = lastAirDate.toLocalDateOrNull(),
        inProduction = inProduction,
        isOverviewFallback = false,
        cast = aggregateCredits?.cast.orEmpty().toAggregateCastMembers(),
        // Recommendations of a series may contain movies, so the item type is kept when present.
        recommendations = recommendations.toMediaList(MediaType.TV),
        similar = similar.toMediaList(MediaType.TV),
    )
}

/** First [MAX_CAST_MEMBERS] actors in billing order; an actor with two roles is listed once. */
fun List<CastDto>.toCastMembers(): List<CastMember> = mapIndexedNotNull { index, dto ->
    val personId = dto.id ?: return@mapIndexedNotNull null
    val name = dto.name.nullIfBlank() ?: return@mapIndexedNotNull null
    CastMember(
        personId = personId,
        name = name,
        character = dto.character.nullIfBlank(),
        profilePath = dto.profilePath.nullIfBlank(),
        order = dto.order ?: index,
    )
}.normalizeCast()

/** Series cast: the role with the most episodes is shown. */
fun List<AggregateCastDto>.toAggregateCastMembers(): List<CastMember> = mapIndexedNotNull { index, dto ->
    val personId = dto.id ?: return@mapIndexedNotNull null
    val name = dto.name.nullIfBlank() ?: return@mapIndexedNotNull null
    val mainRole = dto.roles.orEmpty()
        .filter { !it.character.isNullOrBlank() }
        .maxByOrNull { it.episodeCount ?: 0 }
    CastMember(
        personId = personId,
        name = name,
        character = mainRole?.character?.trim(),
        profilePath = dto.profilePath.nullIfBlank(),
        order = dto.order ?: index,
    )
}.normalizeCast()

private fun List<CastMember>.normalizeCast(): List<CastMember> = sortedBy { it.order }
    .distinctBy { it.personId }
    .take(MAX_CAST_MEMBERS)

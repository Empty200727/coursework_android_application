package ru.kinopolka.testing

import java.time.LocalDate
import ru.kinopolka.core.model.Media
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.network.model.MediaListItemDto
import ru.kinopolka.core.network.model.PagedResponseDto

fun movieDto(
    id: Int,
    popularity: Double = 1.0,
    voteAverage: Double = 7.0,
    releaseDate: String = "2020-01-01",
    mediaType: String? = null,
) = MediaListItemDto(
    id = id,
    mediaType = mediaType,
    title = "Фильм $id",
    releaseDate = releaseDate,
    popularity = popularity,
    voteAverage = voteAverage,
    voteCount = 500,
    genreIds = listOf(28),
)

fun tvDto(
    id: Int,
    popularity: Double = 1.0,
    voteAverage: Double = 7.0,
    firstAirDate: String = "2020-01-01",
    mediaType: String? = null,
) = MediaListItemDto(
    id = id,
    mediaType = mediaType,
    name = "Сериал $id",
    firstAirDate = firstAirDate,
    popularity = popularity,
    voteAverage = voteAverage,
    voteCount = 500,
    genreIds = listOf(18),
)

fun pageOf(items: List<MediaListItemDto>, page: Int = 1, totalPages: Int = 1) =
    PagedResponseDto(page = page, results = items, totalPages = totalPages, totalResults = items.size)

fun testMedia(
    id: Int,
    type: MediaType = MediaType.MOVIE,
    popularity: Double = 1.0,
    voteAverage: Double = 7.0,
    releaseDate: LocalDate? = LocalDate.of(2020, 1, 1),
) = Media(
    key = MediaKey(type, id),
    title = "Произведение $id",
    originalTitle = null,
    overview = null,
    posterPath = null,
    backdropPath = null,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = 500,
    popularity = popularity,
    genreIds = emptyList(),
)

package ru.kinopolka.core.data

import java.util.Locale
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.TmdbGenre

/**
 * Unified genre catalog (F-04, docs/PLAN.md, section 4).
 *
 * TMDB uses different ids and partly different genres for movies and series. The static
 * table below links them: for the «Все» filter the user sees one genre while the app sends
 * the right id for each type. Names are taken from TMDB answers in Russian; [fallbackName]
 * is used only if TMDB does not return a name.
 */
object GenreCatalog {

    data class Mapping(val key: String, val movieGenreId: Int?, val tvGenreId: Int?, val fallbackName: String)

    val mappings: List<Mapping> = listOf(
        Mapping(key = "action", movieGenreId = 28, tvGenreId = 10759, fallbackName = "Боевик"),
        Mapping(key = "comedy", movieGenreId = 35, tvGenreId = 35, fallbackName = "Комедия"),
        Mapping(key = "drama", movieGenreId = 18, tvGenreId = 18, fallbackName = "Драма"),
        Mapping(key = "animation", movieGenreId = 16, tvGenreId = 16, fallbackName = "Мультфильм"),
        Mapping(key = "fantasy", movieGenreId = 14, tvGenreId = 10765, fallbackName = "Фэнтези"),
        Mapping(key = "science_fiction", movieGenreId = 878, tvGenreId = 10765, fallbackName = "Фантастика"),
        Mapping(key = "crime", movieGenreId = 80, tvGenreId = 80, fallbackName = "Криминал"),
        Mapping(key = "mystery", movieGenreId = 9648, tvGenreId = 9648, fallbackName = "Детектив"),
        Mapping(key = "thriller", movieGenreId = 53, tvGenreId = null, fallbackName = "Триллер"),
        Mapping(key = "horror", movieGenreId = 27, tvGenreId = null, fallbackName = "Ужасы"),
        Mapping(key = "adventure", movieGenreId = 12, tvGenreId = 10759, fallbackName = "Приключения"),
        Mapping(key = "family", movieGenreId = 10751, tvGenreId = 10751, fallbackName = "Семейный"),
        Mapping(key = "romance", movieGenreId = 10749, tvGenreId = null, fallbackName = "Мелодрама"),
        Mapping(key = "documentary", movieGenreId = 99, tvGenreId = 99, fallbackName = "Документальный"),
        Mapping(key = "war", movieGenreId = 10752, tvGenreId = 10768, fallbackName = "Военный"),
        Mapping(key = "history", movieGenreId = 36, tvGenreId = null, fallbackName = "История"),
        Mapping(key = "western", movieGenreId = 37, tvGenreId = 37, fallbackName = "Вестерн"),
        Mapping(key = "music", movieGenreId = 10402, tvGenreId = null, fallbackName = "Музыка"),
        Mapping(key = "tv_movie", movieGenreId = 10770, tvGenreId = null, fallbackName = "Телевизионный фильм"),
        Mapping(key = "kids", movieGenreId = null, tvGenreId = 10762, fallbackName = "Детский"),
        Mapping(key = "reality", movieGenreId = null, tvGenreId = 10764, fallbackName = "Реалити-шоу"),
        Mapping(key = "soap", movieGenreId = null, tvGenreId = 10766, fallbackName = "Мыльная опера"),
        Mapping(key = "talk", movieGenreId = null, tvGenreId = 10767, fallbackName = "Ток-шоу"),
        Mapping(key = "news", movieGenreId = null, tvGenreId = 10763, fallbackName = "Новости"),
    )

    private val russian: Locale = Locale.forLanguageTag("ru")

    /**
     * Builds the unified catalog from the TMDB genre lists of both types.
     *
     * - A mapped id that TMDB no longer returns is dropped; a mapping with no ids left is dropped.
     * - A TMDB genre missing from the table becomes a separate one-type genre (`movie-<id>`,
     *   `tv-<id>`), so a new TMDB genre is never lost.
     * - The name of the movie genre wins: «Боевик» rather than «Боевик и Приключения».
     */
    fun merge(genres: List<TmdbGenre>): List<Genre> {
        val names: Map<Pair<MediaType, Int>, String> = genres.associate { (it.mediaType to it.id) to it.name }
        fun known(type: MediaType, id: Int?): Int? = id?.takeIf { (type to it) in names }

        val mapped = mappings.mapNotNull { mapping ->
            val movieId = known(MediaType.MOVIE, mapping.movieGenreId)
            val tvId = known(MediaType.TV, mapping.tvGenreId)
            if (movieId == null && tvId == null) return@mapNotNull null
            val name = movieId?.let { names[MediaType.MOVIE to it] }
                ?: tvId?.let { names[MediaType.TV to it] }
                ?: mapping.fallbackName
            Genre(key = mapping.key, name = name.capitalized(), movieGenreId = movieId, tvGenreId = tvId)
        }

        val mappedIds = mappings.flatMapTo(mutableSetOf()) { mapping ->
            listOfNotNull(
                mapping.movieGenreId?.let { MediaType.MOVIE to it },
                mapping.tvGenreId?.let { MediaType.TV to it },
            )
        }
        val unmapped = genres
            .filter { (it.mediaType to it.id) !in mappedIds }
            .distinctBy { it.mediaType to it.id }
            .map { genre ->
                Genre(
                    key = "${genre.mediaType.key}-${genre.id}",
                    name = genre.name.capitalized(),
                    movieGenreId = genre.id.takeIf { genre.mediaType == MediaType.MOVIE },
                    tvGenreId = genre.id.takeIf { genre.mediaType == MediaType.TV },
                )
            }
        return mapped + unmapped
    }

    /** TMDB returns Russian genre names in lower case («боевик»). */
    private fun String.capitalized(): String = trim().replaceFirstChar { it.titlecase(russian) }
}

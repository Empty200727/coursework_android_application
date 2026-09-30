package ru.kinopolka.core.model

/** A horizontal collection of the home screen (F-05, F-07). */
sealed interface Shelf {
    /** «В тренде за неделю» for the selected filter. */
    data class Trending(val filter: MediaFilter) : Shelf

    /** «Популярные фильмы» or «Популярные сериалы». */
    data class Popular(val mediaType: MediaType) : Shelf

    /** First 20 titles of a genre; with «Все» movies and series are merged by popularity. */
    data class ByGenre(val genre: Genre, val filter: MediaFilter) : Shelf
}

/** Sort orders of the genre screen (F-06). */
enum class MediaSort {
    POPULARITY,
    RATING,
    NEWEST,
}

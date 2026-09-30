package ru.kinopolka.core.model

/** The «Все / Фильмы / Сериалы» filter (F-02). */
enum class MediaFilter(val mediaTypes: Set<MediaType>) {
    ALL(setOf(MediaType.MOVIE, MediaType.TV)),
    MOVIES(setOf(MediaType.MOVIE)),
    SERIES(setOf(MediaType.TV)),
    ;

    fun includes(type: MediaType): Boolean = type in mediaTypes
}

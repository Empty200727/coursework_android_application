package ru.kinopolka.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.Genre
import ru.kinopolka.core.model.MediaFilter

/** Unified genre catalog (F-04), stored in Room and refreshed from TMDB once in 7 days. */
interface GenreRepository {

    /** Genres from the local database; empty until the first successful refresh. */
    fun observeGenres(): Flow<List<Genre>>

    fun observeGenres(filter: MediaFilter): Flow<List<Genre>> =
        observeGenres().map { genres -> genres.filter { it.matches(filter) } }

    suspend fun getGenre(key: String): Genre?

    /**
     * Loads genres from TMDB if the cache is missing or older than 7 days, or if [force].
     * A failed refresh keeps the cached genres.
     */
    suspend fun refresh(force: Boolean = false): RefreshResult
}

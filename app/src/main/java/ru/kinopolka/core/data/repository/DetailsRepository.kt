package ru.kinopolka.core.data.repository

import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.data.RefreshResult
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaKey

/** Title card (F-08…F-10): read from Room, loaded from TMDB with one request and cached for 24 hours. */
interface DetailsRepository {

    /** `null` while nothing is known about the title. */
    fun observeDetails(key: MediaKey): Flow<MediaDetails?>

    suspend fun refreshDetails(key: MediaKey, force: Boolean = false): RefreshResult
}

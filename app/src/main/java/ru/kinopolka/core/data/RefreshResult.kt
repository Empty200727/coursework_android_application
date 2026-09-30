package ru.kinopolka.core.data

import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Why data could not be loaded; screens turn it into a message (N-06). */
enum class DataError {
    NO_CONNECTION,
    UNAUTHORIZED,
    NOT_FOUND,
    SERVER,
    BAD_RESPONSE,
}

/** Outcome of a cache refresh. */
sealed interface RefreshResult {
    /** The cache was fresh, the network was not used. */
    data object Skipped : RefreshResult

    data object Updated : RefreshResult

    data class Failed(val error: DataError) : RefreshResult
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

/**
 * Runs a refresh that returns `true` when it used the network and converts expected network
 * failures into [RefreshResult.Failed]. Other exceptions (including cancellation) propagate.
 */
suspend fun runRefresh(block: suspend () -> Boolean): RefreshResult = try {
    if (block()) RefreshResult.Updated else RefreshResult.Skipped
} catch (e: IOException) {
    RefreshResult.Failed(e.toDataError())
} catch (e: HttpException) {
    RefreshResult.Failed(e.toDataError())
} catch (e: SerializationException) {
    RefreshResult.Failed(e.toDataError())
}

fun Throwable.toDataError(): DataError = when (this) {
    is HttpException -> when (code()) {
        HTTP_UNAUTHORIZED -> DataError.UNAUTHORIZED
        HTTP_NOT_FOUND -> DataError.NOT_FOUND
        else -> DataError.SERVER
    }

    is SerializationException -> DataError.BAD_RESPONSE

    is IOException -> DataError.NO_CONNECTION

    else -> DataError.SERVER
}

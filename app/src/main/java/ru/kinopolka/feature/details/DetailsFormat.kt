package ru.kinopolka.feature.details

import java.text.NumberFormat
import java.util.Locale
import ru.kinopolka.core.model.MediaDetails
import ru.kinopolka.core.model.MediaType

private const val MINUTES_IN_HOUR = 60

/** «1999» for a movie; «2008–2013» or «2019–н. в.» for a series ([ongoing] is the «н. в.» text). */
internal fun MediaDetails.yearsText(ongoing: String): String? {
    val start = media.releaseYear ?: return null
    if (media.mediaType == MediaType.MOVIE) return start.toString()
    val end = when {
        inProduction == true -> ongoing
        else -> lastAirDate?.year?.takeIf { it != start }?.toString()
    }
    return if (end == null) start.toString() else "$start–$end"
}

/** Runtime as hours and minutes: (2, 19) for 139 minutes. */
internal fun splitRuntime(minutes: Int): Pair<Int, Int> = minutes / MINUTES_IN_HOUR to minutes % MINUTES_IN_HOUR

/** «29 870» — grouped the Russian way. */
internal fun formatCount(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(value)

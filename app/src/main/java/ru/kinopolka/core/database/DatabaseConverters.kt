package ru.kinopolka.core.database

import androidx.room.TypeConverter
import ru.kinopolka.core.model.MediaType

/** Stores [MediaType] as the TMDB key (`movie`, `tv`) rather than the enum name. */
class DatabaseConverters {

    @TypeConverter
    fun mediaTypeToKey(type: MediaType): String = type.key

    @TypeConverter
    fun keyToMediaType(key: String): MediaType = requireNotNull(MediaType.fromKey(key)) { "Unknown media type: $key" }
}

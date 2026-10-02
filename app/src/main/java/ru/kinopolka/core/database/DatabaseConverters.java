package ru.kinopolka.core.database;

import androidx.room.TypeConverter;
import ru.kinopolka.core.model.MediaType;

/** Stores {@link MediaType} as the TMDB key ({@code movie}, {@code tv}) rather than the enum name. */
public final class DatabaseConverters {

    private DatabaseConverters() {
    }

    @TypeConverter
    public static String mediaTypeToKey(MediaType type) {
        return type.key();
    }

    @TypeConverter
    public static MediaType keyToMediaType(String key) {
        MediaType type = MediaType.fromKey(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown media type: " + key);
        }
        return type;
    }
}

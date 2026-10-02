package ru.kinopolka.core.data;

import com.google.gson.JsonParseException;
import java.io.IOException;
import retrofit2.HttpException;

/** Why data could not be loaded; screens turn it into a message (N-06). */
public enum DataError {
    NO_CONNECTION,
    UNAUTHORIZED,
    NOT_FOUND,
    SERVER,
    BAD_RESPONSE;

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_NOT_FOUND = 404;

    public static DataError of(Throwable error) {
        if (error instanceof HttpException http) {
            return switch (http.code()) {
                case HTTP_UNAUTHORIZED -> UNAUTHORIZED;
                case HTTP_NOT_FOUND -> NOT_FOUND;
                default -> SERVER;
            };
        }
        if (error instanceof JsonParseException) {
            return BAD_RESPONSE;
        }
        if (error instanceof IOException) {
            return NO_CONNECTION;
        }
        return SERVER;
    }

    /** Errors expected from TMDB calls; anything else is a bug and must not be hidden. */
    public static boolean isExpected(Throwable error) {
        return error instanceof IOException || error instanceof HttpException || error instanceof JsonParseException;
    }
}

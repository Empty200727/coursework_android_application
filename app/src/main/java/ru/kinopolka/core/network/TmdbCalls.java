package ru.kinopolka.core.network;

import com.google.gson.JsonParseException;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;

/** Runs a Retrofit call on the current (background) thread. */
public final class TmdbCalls {

    private TmdbCalls() {
    }

    /**
     * Returns the body of a successful response.
     *
     * @throws IOException without connection
     * @throws HttpException when TMDB answers with an error code
     * @throws JsonParseException when the body is missing or malformed
     */
    public static <T> T execute(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (!response.isSuccessful()) {
            throw new HttpException(response);
        }
        T body = response.body();
        if (body == null) {
            throw new JsonParseException("Empty response of " + call.request().url());
        }
        return body;
    }
}

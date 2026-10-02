package ru.kinopolka.testing;

import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import ru.kinopolka.core.network.TmdbNetwork;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.PagedResponseDto;

/** JSON responses of TMDB stored in src/test/resources/fixtures. */
public final class Fixtures {

    private static final Type PAGE_TYPE = new TypeToken<PagedResponseDto<MediaListItemDto>>() { }.getType();

    private Fixtures() {
    }

    public static String read(String name) {
        try (InputStream stream = Fixtures.class.getClassLoader().getResourceAsStream("fixtures/" + name)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture " + name + " not found");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T parse(String name, Class<T> type) {
        return TmdbNetwork.gson().fromJson(read(name), type);
    }

    public static PagedResponseDto<MediaListItemDto> page(String name) {
        return TmdbNetwork.gson().fromJson(read(name), PAGE_TYPE);
    }
}

package ru.kinopolka.core.network;

/** TMDB image sizes (N-04): {@code w342} for lists, {@code w500} for the title card. */
public enum ImageSize {
    POSTER_LIST("w342"),
    POSTER_DETAILS("w500"),
    BACKDROP("w780"),
    PROFILE("w185");

    private final String value;

    ImageSize(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

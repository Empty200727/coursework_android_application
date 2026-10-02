package ru.kinopolka.core.network;

/** Sort orders of discover; release date fields differ for movies and series. */
public enum DiscoverSort {
    POPULARITY("popularity.desc", "popularity.desc"),
    RATING("vote_average.desc", "vote_average.desc"),
    NEWEST("primary_release_date.desc", "first_air_date.desc");

    private final String movieValue;
    private final String tvValue;

    DiscoverSort(String movieValue, String tvValue) {
        this.movieValue = movieValue;
        this.tvValue = tvValue;
    }

    public String movieValue() {
        return movieValue;
    }

    public String tvValue() {
        return tvValue;
    }
}

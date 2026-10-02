package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * An item of search, trending, popular and discover results. Movies use {@code title},
 * {@code original_title} and {@code release_date}, series use {@code name}, {@code original_name}
 * and {@code first_air_date}. {@code media_type} is present only in search/multi and trending
 * (and can be {@code person}).
 */
public record MediaListItemDto(
        @SerializedName("id") @Nullable Integer id,
        @SerializedName("media_type") @Nullable String mediaType,
        @SerializedName("title") @Nullable String title,
        @SerializedName("name") @Nullable String name,
        @SerializedName("original_title") @Nullable String originalTitle,
        @SerializedName("original_name") @Nullable String originalName,
        @SerializedName("overview") @Nullable String overview,
        @SerializedName("poster_path") @Nullable String posterPath,
        @SerializedName("backdrop_path") @Nullable String backdropPath,
        @SerializedName("release_date") @Nullable String releaseDate,
        @SerializedName("first_air_date") @Nullable String firstAirDate,
        @SerializedName("vote_average") @Nullable Double voteAverage,
        @SerializedName("vote_count") @Nullable Integer voteCount,
        @SerializedName("popularity") @Nullable Double popularity,
        @SerializedName("genre_ids") @Nullable List<Integer> genreIds,
        @SerializedName("adult") @Nullable Boolean adult) {
}

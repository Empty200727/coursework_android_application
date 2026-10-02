package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** movie/{id}?append_to_response=credits,recommendations,similar */
public record MovieDetailsDto(
        @SerializedName("id") @Nullable Integer id,
        @SerializedName("title") @Nullable String title,
        @SerializedName("original_title") @Nullable String originalTitle,
        @SerializedName("overview") @Nullable String overview,
        @SerializedName("poster_path") @Nullable String posterPath,
        @SerializedName("backdrop_path") @Nullable String backdropPath,
        @SerializedName("release_date") @Nullable String releaseDate,
        @SerializedName("runtime") @Nullable Integer runtime,
        @SerializedName("vote_average") @Nullable Double voteAverage,
        @SerializedName("vote_count") @Nullable Integer voteCount,
        @SerializedName("popularity") @Nullable Double popularity,
        @SerializedName("genres") @Nullable List<GenreDto> genres,
        @SerializedName("credits") @Nullable CreditsDto credits,
        @SerializedName("recommendations") @Nullable PagedResponseDto<MediaListItemDto> recommendations,
        @SerializedName("similar") @Nullable PagedResponseDto<MediaListItemDto> similar) {

    public MovieDetailsDto withOverview(@Nullable String newOverview) {
        return new MovieDetailsDto(id, title, originalTitle, newOverview, posterPath, backdropPath, releaseDate,
                runtime, voteAverage, voteCount, popularity, genres, credits, recommendations, similar);
    }

    public MovieDetailsDto withRecommendations(@Nullable PagedResponseDto<MediaListItemDto> newRecommendations) {
        return new MovieDetailsDto(id, title, originalTitle, overview, posterPath, backdropPath, releaseDate,
                runtime, voteAverage, voteCount, popularity, genres, credits, newRecommendations, similar);
    }
}

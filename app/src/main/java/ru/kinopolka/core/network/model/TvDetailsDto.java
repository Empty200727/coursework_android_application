package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** tv/{id}?append_to_response=aggregate_credits,recommendations,similar */
public record TvDetailsDto(
        @SerializedName("id") @Nullable Integer id,
        @SerializedName("name") @Nullable String name,
        @SerializedName("original_name") @Nullable String originalName,
        @SerializedName("overview") @Nullable String overview,
        @SerializedName("poster_path") @Nullable String posterPath,
        @SerializedName("backdrop_path") @Nullable String backdropPath,
        @SerializedName("first_air_date") @Nullable String firstAirDate,
        @SerializedName("last_air_date") @Nullable String lastAirDate,
        @SerializedName("number_of_seasons") @Nullable Integer numberOfSeasons,
        @SerializedName("in_production") @Nullable Boolean inProduction,
        @SerializedName("vote_average") @Nullable Double voteAverage,
        @SerializedName("vote_count") @Nullable Integer voteCount,
        @SerializedName("popularity") @Nullable Double popularity,
        @SerializedName("genres") @Nullable List<GenreDto> genres,
        @SerializedName("aggregate_credits") @Nullable AggregateCreditsDto aggregateCredits,
        @SerializedName("recommendations") @Nullable PagedResponseDto<MediaListItemDto> recommendations,
        @SerializedName("similar") @Nullable PagedResponseDto<MediaListItemDto> similar) {

    public TvDetailsDto withOverview(@Nullable String newOverview) {
        return new TvDetailsDto(id, name, originalName, newOverview, posterPath, backdropPath, firstAirDate,
                lastAirDate, numberOfSeasons, inProduction, voteAverage, voteCount, popularity, genres,
                aggregateCredits, recommendations, similar);
    }
}

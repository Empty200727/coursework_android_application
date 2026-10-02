package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Series credits summed over all seasons: an actor may have several roles. */
public record AggregateCastDto(
        @SerializedName("id") @Nullable Integer id,
        @SerializedName("name") @Nullable String name,
        @SerializedName("profile_path") @Nullable String profilePath,
        @SerializedName("roles") @Nullable List<RoleDto> roles,
        @SerializedName("total_episode_count") @Nullable Integer totalEpisodeCount,
        @SerializedName("order") @Nullable Integer order) {
}

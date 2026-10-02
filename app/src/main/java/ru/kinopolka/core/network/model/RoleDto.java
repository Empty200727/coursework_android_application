package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public record RoleDto(
        @SerializedName("character") @Nullable String character,
        @SerializedName("episode_count") @Nullable Integer episodeCount) {
}

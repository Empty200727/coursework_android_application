package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public record CastDto(
        @SerializedName("id") @Nullable Integer id,
        @SerializedName("name") @Nullable String name,
        @SerializedName("character") @Nullable String character,
        @SerializedName("profile_path") @Nullable String profilePath,
        @SerializedName("order") @Nullable Integer order) {
}

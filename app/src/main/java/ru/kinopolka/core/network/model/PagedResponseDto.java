package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public record PagedResponseDto<T>(
        @SerializedName("page") @Nullable Integer page,
        @SerializedName("results") @Nullable List<T> results,
        @SerializedName("total_pages") @Nullable Integer totalPages,
        @SerializedName("total_results") @Nullable Integer totalResults) {
}

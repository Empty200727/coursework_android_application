package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public record GenreListResponseDto(@SerializedName("genres") @Nullable List<GenreDto> genres) {
}

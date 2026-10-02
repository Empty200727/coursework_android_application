package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

// All DTO fields are nullable: TMDB omits fields freely and may change the response format,
// a partial answer must still be parsed (risk «TMDB меняет формат ответа»).
public record GenreDto(@SerializedName("id") @Nullable Integer id, @SerializedName("name") @Nullable String name) {
}

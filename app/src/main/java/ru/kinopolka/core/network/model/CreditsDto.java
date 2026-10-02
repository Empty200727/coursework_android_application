package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public record CreditsDto(@SerializedName("cast") @Nullable List<CastDto> cast) {
}

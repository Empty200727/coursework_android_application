package ru.kinopolka.core.network.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public record AggregateCreditsDto(@SerializedName("cast") @Nullable List<AggregateCastDto> cast) {
}

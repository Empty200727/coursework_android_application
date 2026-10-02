package ru.kinopolka.core.data.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import java.util.List;
import java.util.function.Function;

/** Combining of LiveData sources, the counterpart of {@code combine} for flows. */
public final class LiveDataUtils {

    private LiveDataUtils() {
    }

    /**
     * Emits {@code combiner(values)} each time a source changes, once every source has a value.
     * Values are passed in the order of {@code sources}.
     */
    public static <R> LiveData<R> combine(List<? extends LiveData<?>> sources, Function<Object[], R> combiner) {
        MediatorLiveData<R> result = new MediatorLiveData<>();
        Object[] values = new Object[sources.size()];
        boolean[] received = new boolean[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            int index = i;
            result.addSource(sources.get(i), value -> {
                values[index] = value;
                received[index] = true;
                for (boolean ready : received) {
                    if (!ready) {
                        return;
                    }
                }
                result.setValue(combiner.apply(values.clone()));
            });
        }
        return result;
    }

    public static <A, B, R> LiveData<R> combine(LiveData<A> first, LiveData<B> second, Combiner2<A, B, R> combiner) {
        return combine(List.of(first, second), values -> combiner.apply(cast(values[0]), cast(values[1])));
    }

    @FunctionalInterface
    public interface Combiner2<A, B, R> {
        R apply(A first, B second);
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value) {
        return (T) value;
    }
}

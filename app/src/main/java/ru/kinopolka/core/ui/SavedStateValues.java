package ru.kinopolka.core.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

/** Enum values in {@link SavedStateHandle}: stored by name, so they survive process death (N-05). */
public final class SavedStateValues {

    private SavedStateValues() {
    }

    public static <E extends Enum<E>> LiveData<E> enumLiveData(SavedStateHandle handle, String key, E defaultValue) {
        LiveData<String> names = handle.getLiveData(key, defaultValue.name());
        return Transformations.distinctUntilChanged(
                Transformations.map(names, name -> parse(name, defaultValue)));
    }

    public static <E extends Enum<E>> E get(SavedStateHandle handle, String key, E defaultValue) {
        return parse(handle.get(key), defaultValue);
    }

    public static <E extends Enum<E>> E parse(String name, E defaultValue) {
        for (E value : defaultValue.getDeclaringClass().getEnumConstants()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return defaultValue;
    }
}

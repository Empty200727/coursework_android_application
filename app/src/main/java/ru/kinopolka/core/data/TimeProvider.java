package ru.kinopolka.core.data;

/** Source of the current time, replaced by a fake in tests. */
@FunctionalInterface
public interface TimeProvider {

    TimeProvider SYSTEM = System::currentTimeMillis;

    long nowMillis();
}

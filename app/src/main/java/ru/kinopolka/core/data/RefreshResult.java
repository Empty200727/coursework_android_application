package ru.kinopolka.core.data;

import java.io.IOException;
import java.util.List;

/** Outcome of a cache refresh. */
public sealed interface RefreshResult permits RefreshResult.Skipped, RefreshResult.Updated, RefreshResult.Failed {

    /** The cache was fresh, the network was not used. */
    RefreshResult SKIPPED = new Skipped();

    RefreshResult UPDATED = new Updated();

    record Skipped() implements RefreshResult {
    }

    record Updated() implements RefreshResult {
    }

    record Failed(DataError error) implements RefreshResult {
    }

    /** A refresh body: returns {@code true} when it used the network. */
    @FunctionalInterface
    interface Body {
        boolean run() throws IOException;
    }

    /**
     * Runs {@code body} and converts expected network failures into {@link Failed}.
     * Other exceptions propagate.
     */
    static RefreshResult run(Body body) {
        try {
            return body.run() ? UPDATED : SKIPPED;
        } catch (IOException | RuntimeException e) {
            if (!DataError.isExpected(e)) {
                throw e instanceof RuntimeException runtime ? runtime : new IllegalStateException(e);
            }
            return new Failed(DataError.of(e));
        }
    }

    /** The first failure, otherwise {@link #UPDATED} if anything was updated, otherwise {@link #SKIPPED}. */
    static RefreshResult combine(List<RefreshResult> results) {
        for (RefreshResult result : results) {
            if (result instanceof Failed) {
                return result;
            }
        }
        for (RefreshResult result : results) {
            if (result instanceof Updated) {
                return result;
            }
        }
        return SKIPPED;
    }
}

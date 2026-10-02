package ru.kinopolka.core.data.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutorService;

/**
 * Background threads for database writes and network calls. Repositories block; ViewModels and
 * paging sources run them here. Tests use {@link #direct()} to run everything on the calling thread.
 */
public final class AppExecutors {

    private final ListeningExecutorService io;

    public AppExecutors(ExecutorService io) {
        this.io = MoreExecutors.listeningDecorator(io);
    }

    public static AppExecutors direct() {
        return new AppExecutors(MoreExecutors.newDirectExecutorService());
    }

    public ListeningExecutorService io() {
        return io;
    }
}

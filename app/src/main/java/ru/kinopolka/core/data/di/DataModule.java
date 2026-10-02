package ru.kinopolka.core.data.di;

import android.content.Context;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import ru.kinopolka.BuildConfig;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.library.FilePosterStorage;
import ru.kinopolka.core.data.library.PosterStorage;
import ru.kinopolka.core.data.repository.DetailsRepository;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.data.repository.LibraryRepository;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.data.repository.OfflineFirstDetailsRepository;
import ru.kinopolka.core.data.repository.OfflineFirstGenreRepository;
import ru.kinopolka.core.data.repository.OfflineFirstLibraryRepository;
import ru.kinopolka.core.data.repository.OfflineFirstMediaRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.ConnectivityManagerNetworkMonitor;
import ru.kinopolka.core.data.util.NetworkMonitor;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DataModule {

    /** Network calls wait most of the time, so the pool is larger than the number of cores. */
    private static final int IO_THREADS = 8;

    @Binds
    abstract GenreRepository bindGenreRepository(OfflineFirstGenreRepository repository);

    @Binds
    abstract MediaRepository bindMediaRepository(OfflineFirstMediaRepository repository);

    @Binds
    abstract DetailsRepository bindDetailsRepository(OfflineFirstDetailsRepository repository);

    @Binds
    abstract LibraryRepository bindLibraryRepository(OfflineFirstLibraryRepository repository);

    @Binds
    abstract NetworkMonitor bindNetworkMonitor(ConnectivityManagerNetworkMonitor monitor);

    @Provides
    static TimeProvider provideTimeProvider() {
        return TimeProvider.SYSTEM;
    }

    @Provides
    @Singleton
    static AppExecutors provideAppExecutors() {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "kinopolka-io-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new AppExecutors(Executors.newFixedThreadPool(IO_THREADS, factory));
    }

    @Provides
    @Singleton
    static PosterStorage providePosterStorage(@ApplicationContext Context context, OkHttpClient client) {
        return new FilePosterStorage(new File(context.getFilesDir(), "posters"), client,
                BuildConfig.TMDB_IMAGE_BASE_URL);
    }
}

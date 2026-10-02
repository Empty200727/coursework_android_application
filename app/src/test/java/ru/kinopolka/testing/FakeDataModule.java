package ru.kinopolka.testing;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;
import javax.inject.Singleton;
import ru.kinopolka.core.data.TimeProvider;
import ru.kinopolka.core.data.di.DataModule;
import ru.kinopolka.core.data.library.PosterStorage;
import ru.kinopolka.core.data.repository.DetailsRepository;
import ru.kinopolka.core.data.repository.GenreRepository;
import ru.kinopolka.core.data.repository.LibraryRepository;
import ru.kinopolka.core.data.repository.MediaRepository;
import ru.kinopolka.core.data.util.AppExecutors;
import ru.kinopolka.core.data.util.NetworkMonitor;

/** Replaces the repositories of the app with fakes in Hilt tests; tests inject the fakes to fill them. */
@Module
@TestInstallIn(components = SingletonComponent.class, replaces = DataModule.class)
public final class FakeDataModule {

    private FakeDataModule() {
    }

    @Provides
    @Singleton
    static FakeGenreRepository fakeGenreRepository() {
        return new FakeGenreRepository();
    }

    @Provides
    @Singleton
    static FakeMediaRepository fakeMediaRepository() {
        return new FakeMediaRepository();
    }

    @Provides
    @Singleton
    static FakeDetailsRepository fakeDetailsRepository() {
        return new FakeDetailsRepository();
    }

    @Provides
    @Singleton
    static FakeLibraryRepository fakeLibraryRepository() {
        return new FakeLibraryRepository();
    }

    @Provides
    @Singleton
    static FakeNetworkMonitor fakeNetworkMonitor() {
        return new FakeNetworkMonitor();
    }

    @Provides
    @Singleton
    static FakePosterStorage fakePosterStorage() {
        return new FakePosterStorage();
    }

    @Provides
    static GenreRepository genreRepository(FakeGenreRepository fake) {
        return fake;
    }

    @Provides
    static MediaRepository mediaRepository(FakeMediaRepository fake) {
        return fake;
    }

    @Provides
    static DetailsRepository detailsRepository(FakeDetailsRepository fake) {
        return fake;
    }

    @Provides
    static LibraryRepository libraryRepository(FakeLibraryRepository fake) {
        return fake;
    }

    @Provides
    static NetworkMonitor networkMonitor(FakeNetworkMonitor fake) {
        return fake;
    }

    @Provides
    static PosterStorage posterStorage(FakePosterStorage fake) {
        return fake;
    }

    @Provides
    static TimeProvider timeProvider() {
        return new TestTimeProvider();
    }

    /** Background work runs on the calling thread: UI tests see its results at once. */
    @Provides
    @Singleton
    static AppExecutors appExecutors() {
        return AppExecutors.direct();
    }
}

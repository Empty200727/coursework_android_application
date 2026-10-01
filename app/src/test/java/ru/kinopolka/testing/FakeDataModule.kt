package ru.kinopolka.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.di.DataModule
import ru.kinopolka.core.data.library.PosterStorage
import ru.kinopolka.core.data.repository.DetailsRepository
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.LibraryRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.util.NetworkMonitor

/** Replaces the repositories of the app with fakes in Hilt tests; tests inject the fakes to fill them. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object FakeDataModule {

    @Provides @Singleton
    fun fakeGenreRepository() = FakeGenreRepository()

    @Provides @Singleton
    fun fakeMediaRepository() = FakeMediaRepository()

    @Provides @Singleton
    fun fakeDetailsRepository() = FakeDetailsRepository()

    @Provides @Singleton
    fun fakeLibraryRepository() = FakeLibraryRepository()

    @Provides @Singleton
    fun fakeNetworkMonitor() = FakeNetworkMonitor()

    @Provides @Singleton
    fun fakePosterStorage() = FakePosterStorage()

    @Provides
    fun genreRepository(fake: FakeGenreRepository): GenreRepository = fake

    @Provides
    fun mediaRepository(fake: FakeMediaRepository): MediaRepository = fake

    @Provides
    fun detailsRepository(fake: FakeDetailsRepository): DetailsRepository = fake

    @Provides
    fun libraryRepository(fake: FakeLibraryRepository): LibraryRepository = fake

    @Provides
    fun networkMonitor(fake: FakeNetworkMonitor): NetworkMonitor = fake

    @Provides
    fun posterStorage(fake: FakePosterStorage): PosterStorage = fake

    @Provides
    fun timeProvider(): TimeProvider = TestTimeProvider()
}

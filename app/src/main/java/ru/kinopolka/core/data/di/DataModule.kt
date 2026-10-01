package ru.kinopolka.core.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import okhttp3.OkHttpClient
import ru.kinopolka.BuildConfig
import ru.kinopolka.core.data.SystemTimeProvider
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.library.FilePosterStorage
import ru.kinopolka.core.data.library.PosterStorage
import ru.kinopolka.core.data.repository.DetailsRepository
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.LibraryRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.repository.OfflineFirstDetailsRepository
import ru.kinopolka.core.data.repository.OfflineFirstGenreRepository
import ru.kinopolka.core.data.repository.OfflineFirstLibraryRepository
import ru.kinopolka.core.data.repository.OfflineFirstMediaRepository
import ru.kinopolka.core.data.util.ConnectivityManagerNetworkMonitor
import ru.kinopolka.core.data.util.NetworkMonitor

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindGenreRepository(repository: OfflineFirstGenreRepository): GenreRepository

    @Binds
    abstract fun bindMediaRepository(repository: OfflineFirstMediaRepository): MediaRepository

    @Binds
    abstract fun bindDetailsRepository(repository: OfflineFirstDetailsRepository): DetailsRepository

    @Binds
    abstract fun bindLibraryRepository(repository: OfflineFirstLibraryRepository): LibraryRepository

    @Binds
    abstract fun bindNetworkMonitor(monitor: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        fun provideTimeProvider(): TimeProvider = SystemTimeProvider

        @Provides
        @Singleton
        fun providePosterStorage(@ApplicationContext context: Context, client: OkHttpClient): PosterStorage =
            FilePosterStorage(
                directory = File(context.filesDir, "posters"),
                client = client,
                imageBaseUrl = BuildConfig.TMDB_IMAGE_BASE_URL,
            )
    }
}

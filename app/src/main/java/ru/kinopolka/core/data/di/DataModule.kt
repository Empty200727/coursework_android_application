package ru.kinopolka.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kinopolka.core.data.SystemTimeProvider
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.MediaRepository
import ru.kinopolka.core.data.repository.OfflineFirstGenreRepository
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
    abstract fun bindNetworkMonitor(monitor: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        fun provideTimeProvider(): TimeProvider = SystemTimeProvider
    }
}

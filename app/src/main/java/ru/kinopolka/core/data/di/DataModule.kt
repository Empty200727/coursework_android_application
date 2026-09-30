package ru.kinopolka.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kinopolka.core.data.SystemTimeProvider
import ru.kinopolka.core.data.TimeProvider
import ru.kinopolka.core.data.repository.GenreRepository
import ru.kinopolka.core.data.repository.OfflineFirstGenreRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindGenreRepository(repository: OfflineFirstGenreRepository): GenreRepository

    companion object {
        @Provides
        fun provideTimeProvider(): TimeProvider = SystemTimeProvider
    }
}

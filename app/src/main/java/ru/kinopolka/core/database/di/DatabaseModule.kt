package ru.kinopolka.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.kinopolka.core.database.KinopolkaDatabase
import ru.kinopolka.core.database.dao.CastDao
import ru.kinopolka.core.database.dao.FeedDao
import ru.kinopolka.core.database.dao.GenreDao
import ru.kinopolka.core.database.dao.LibraryDao
import ru.kinopolka.core.database.dao.MediaDao
import ru.kinopolka.core.database.dao.RelatedMediaDao
import ru.kinopolka.core.database.dao.SearchHistoryDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KinopolkaDatabase =
        // No fallbackToDestructiveMigration(): losing the library is not acceptable.
        Room.databaseBuilder(context, KinopolkaDatabase::class.java, KinopolkaDatabase.NAME).build()

    @Provides
    fun provideMediaDao(database: KinopolkaDatabase): MediaDao = database.mediaDao()

    @Provides
    fun provideGenreDao(database: KinopolkaDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideCastDao(database: KinopolkaDatabase): CastDao = database.castDao()

    @Provides
    fun provideRelatedMediaDao(database: KinopolkaDatabase): RelatedMediaDao = database.relatedMediaDao()

    @Provides
    fun provideFeedDao(database: KinopolkaDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideLibraryDao(database: KinopolkaDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideSearchHistoryDao(database: KinopolkaDatabase): SearchHistoryDao = database.searchHistoryDao()
}

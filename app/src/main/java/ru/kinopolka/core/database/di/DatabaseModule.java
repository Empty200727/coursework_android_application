package ru.kinopolka.core.database.di;

import android.content.Context;
import androidx.room.Room;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import ru.kinopolka.core.database.KinopolkaDatabase;
import ru.kinopolka.core.database.dao.CastDao;
import ru.kinopolka.core.database.dao.FeedDao;
import ru.kinopolka.core.database.dao.GenreDao;
import ru.kinopolka.core.database.dao.LibraryDao;
import ru.kinopolka.core.database.dao.MediaDao;
import ru.kinopolka.core.database.dao.RelatedMediaDao;
import ru.kinopolka.core.database.dao.SearchHistoryDao;

@Module
@InstallIn(SingletonComponent.class)
public final class DatabaseModule {

    private DatabaseModule() {
    }

    @Provides
    @Singleton
    static KinopolkaDatabase provideDatabase(@ApplicationContext Context context) {
        // No fallbackToDestructiveMigration(): losing the library is not acceptable.
        return Room.databaseBuilder(context, KinopolkaDatabase.class, KinopolkaDatabase.NAME).build();
    }

    @Provides
    static MediaDao provideMediaDao(KinopolkaDatabase database) {
        return database.mediaDao();
    }

    @Provides
    static GenreDao provideGenreDao(KinopolkaDatabase database) {
        return database.genreDao();
    }

    @Provides
    static CastDao provideCastDao(KinopolkaDatabase database) {
        return database.castDao();
    }

    @Provides
    static RelatedMediaDao provideRelatedMediaDao(KinopolkaDatabase database) {
        return database.relatedMediaDao();
    }

    @Provides
    static FeedDao provideFeedDao(KinopolkaDatabase database) {
        return database.feedDao();
    }

    @Provides
    static LibraryDao provideLibraryDao(KinopolkaDatabase database) {
        return database.libraryDao();
    }

    @Provides
    static SearchHistoryDao provideSearchHistoryDao(KinopolkaDatabase database) {
        return database.searchHistoryDao();
    }
}

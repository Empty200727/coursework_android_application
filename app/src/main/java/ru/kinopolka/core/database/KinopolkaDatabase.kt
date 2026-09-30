package ru.kinopolka.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.kinopolka.core.database.dao.CastDao
import ru.kinopolka.core.database.dao.FeedDao
import ru.kinopolka.core.database.dao.GenreDao
import ru.kinopolka.core.database.dao.LibraryDao
import ru.kinopolka.core.database.dao.MediaDao
import ru.kinopolka.core.database.dao.RelatedMediaDao
import ru.kinopolka.core.database.dao.SearchHistoryDao
import ru.kinopolka.core.database.entity.CastMemberEntity
import ru.kinopolka.core.database.entity.FeedItemEntity
import ru.kinopolka.core.database.entity.GenreEntity
import ru.kinopolka.core.database.entity.LibraryEntryEntity
import ru.kinopolka.core.database.entity.MediaEntity
import ru.kinopolka.core.database.entity.MediaGenreEntity
import ru.kinopolka.core.database.entity.RelatedMediaEntity
import ru.kinopolka.core.database.entity.SearchHistoryEntity

/**
 * Schema changes require a new version, a migration and a MigrationTestHelper test:
 * a destructive migration would erase the user's library.
 */
@Database(
    entities = [
        MediaEntity::class,
        MediaGenreEntity::class,
        GenreEntity::class,
        CastMemberEntity::class,
        RelatedMediaEntity::class,
        FeedItemEntity::class,
        LibraryEntryEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class KinopolkaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun genreDao(): GenreDao
    abstract fun castDao(): CastDao
    abstract fun relatedMediaDao(): RelatedMediaDao
    abstract fun feedDao(): FeedDao
    abstract fun libraryDao(): LibraryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val NAME = "kinopolka.db"
    }
}

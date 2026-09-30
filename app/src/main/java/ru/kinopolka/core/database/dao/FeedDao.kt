package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.FeedItemEntity
import ru.kinopolka.core.database.entity.MediaEntity

@Dao
interface FeedDao {

    @Query(
        """
        SELECT media.* FROM feed_item
        INNER JOIN media
            ON media.media_type = feed_item.media_type
           AND media.tmdb_id = feed_item.tmdb_id
        WHERE feed_item.feed_key = :feedKey
        ORDER BY feed_item.position
        """,
    )
    fun observeFeed(feedKey: String): Flow<List<MediaEntity>>

    /** When the feed was loaded, `null` if it has never been loaded. */
    @Query("SELECT MIN(fetched_at) FROM feed_item WHERE feed_key = :feedKey")
    suspend fun fetchedAt(feedKey: String): Long?

    @Query("DELETE FROM feed_item WHERE feed_key = :feedKey")
    suspend fun deleteFeed(feedKey: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<FeedItemEntity>)

    /** The titles of [items] must already be stored in `media`. */
    @Transaction
    suspend fun replaceFeed(feedKey: String, items: List<FeedItemEntity>) {
        deleteFeed(feedKey)
        insertItems(items)
    }
}

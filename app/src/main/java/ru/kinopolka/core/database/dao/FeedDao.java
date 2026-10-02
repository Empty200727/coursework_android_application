package ru.kinopolka.core.database.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
import ru.kinopolka.core.database.entity.FeedItemEntity;
import ru.kinopolka.core.database.entity.MediaEntity;

@Dao
public abstract class FeedDao {

    private static final String FEED_QUERY = "SELECT media.* FROM feed_item"
            + " INNER JOIN media ON media.media_type = feed_item.media_type AND media.tmdb_id = feed_item.tmdb_id"
            + " WHERE feed_item.feed_key = :feedKey ORDER BY feed_item.position";

    @Query(FEED_QUERY)
    public abstract LiveData<List<MediaEntity>> observeFeed(String feedKey);

    @Query(FEED_QUERY)
    public abstract List<MediaEntity> getFeed(String feedKey);

    /** When the feed was loaded, {@code null} if it has never been loaded. */
    @Nullable
    @Query("SELECT MIN(fetched_at) FROM feed_item WHERE feed_key = :feedKey")
    public abstract Long fetchedAt(String feedKey);

    @Query("DELETE FROM feed_item WHERE feed_key = :feedKey")
    protected abstract void deleteFeed(String feedKey);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertItems(List<FeedItemEntity> items);

    /** The titles of the items must already be stored in {@code media}. */
    @Transaction
    public void replaceFeed(String feedKey, List<FeedItemEntity> items) {
        deleteFeed(feedKey);
        insertItems(items);
    }
}

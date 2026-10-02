package ru.kinopolka.core.database.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;
import java.util.List;
import ru.kinopolka.core.database.entity.SearchHistoryEntity;

@Dao
public abstract class SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searched_at DESC LIMIT :limit")
    public abstract List<SearchHistoryEntity> getRecent(int limit);

    @Upsert
    public abstract void upsert(SearchHistoryEntity entry);

    /** Keeps only the most recent queries. */
    @Query("DELETE FROM search_history WHERE `query` NOT IN ("
            + " SELECT `query` FROM search_history ORDER BY searched_at DESC LIMIT :keep)")
    public abstract void trimTo(int keep);

    @Query("DELETE FROM search_history")
    public abstract void clear();
}

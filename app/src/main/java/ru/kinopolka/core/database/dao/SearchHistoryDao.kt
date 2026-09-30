package ru.kinopolka.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.kinopolka.core.database.entity.SearchHistoryEntity

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searched_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Upsert
    suspend fun upsert(entry: SearchHistoryEntity)

    /** Keeps only the [keep] most recent queries. */
    @Query(
        """
        DELETE FROM search_history WHERE `query` NOT IN (
            SELECT `query` FROM search_history ORDER BY searched_at DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

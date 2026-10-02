package ru.kinopolka.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
import ru.kinopolka.core.database.entity.CastMemberEntity;
import ru.kinopolka.core.model.MediaType;

@Dao
public abstract class CastDao {

    @Query("SELECT * FROM cast_member WHERE media_type = :mediaType AND tmdb_id = :tmdbId ORDER BY cast_order")
    public abstract LiveData<List<CastMemberEntity>> observeCast(MediaType mediaType, int tmdbId);

    @Query("SELECT * FROM cast_member WHERE media_type = :mediaType AND tmdb_id = :tmdbId ORDER BY cast_order")
    public abstract List<CastMemberEntity> getCast(MediaType mediaType, int tmdbId);

    @Query("DELETE FROM cast_member WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    protected abstract void deleteCast(MediaType mediaType, int tmdbId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertCast(List<CastMemberEntity> cast);

    @Transaction
    public void replaceCast(MediaType mediaType, int tmdbId, List<CastMemberEntity> cast) {
        deleteCast(mediaType, tmdbId);
        insertCast(cast);
    }
}

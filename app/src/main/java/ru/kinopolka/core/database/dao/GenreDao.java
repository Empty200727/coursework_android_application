package ru.kinopolka.core.database.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;
import java.util.List;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.model.MediaType;

@Dao
public abstract class GenreDao {

    @Query("SELECT * FROM genre ORDER BY media_type, genre_id")
    public abstract LiveData<List<GenreEntity>> observeAll();

    @Query("SELECT * FROM genre ORDER BY media_type, genre_id")
    public abstract List<GenreEntity> getAll();

    /** Oldest write time for the type or {@code null} when there are no genres of that type. */
    @Nullable
    @Query("SELECT MIN(cached_at) FROM genre WHERE media_type = :mediaType")
    public abstract Long oldestCachedAt(MediaType mediaType);

    @Query("DELETE FROM genre WHERE media_type = :mediaType")
    protected abstract void deleteByType(MediaType mediaType);

    @Upsert
    public abstract void upsert(List<GenreEntity> genres);

    /** Replaces the genre list of one type atomically: removed TMDB genres disappear. */
    @Transaction
    public void replaceForType(MediaType mediaType, List<GenreEntity> genres) {
        deleteByType(mediaType);
        upsert(genres);
    }
}

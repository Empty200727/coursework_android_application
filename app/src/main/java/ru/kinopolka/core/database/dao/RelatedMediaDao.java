package ru.kinopolka.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.database.entity.RelatedMediaEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;

@Dao
public abstract class RelatedMediaDao {

    private static final String RELATED_QUERY = "SELECT media.* FROM related_media"
            + " INNER JOIN media ON media.media_type = related_media.target_media_type"
            + " AND media.tmdb_id = related_media.target_tmdb_id"
            + " WHERE related_media.source_media_type = :mediaType AND related_media.source_tmdb_id = :tmdbId"
            + " AND related_media.kind = :kind ORDER BY related_media.position";

    @Query(RELATED_QUERY)
    public abstract LiveData<List<MediaEntity>> observeRelated(MediaType mediaType, int tmdbId, RelatedKind kind);

    @Query(RELATED_QUERY)
    public abstract List<MediaEntity> getRelated(MediaType mediaType, int tmdbId, RelatedKind kind);

    @Query("DELETE FROM related_media"
            + " WHERE source_media_type = :mediaType AND source_tmdb_id = :tmdbId AND kind = :kind")
    protected abstract void deleteRelated(MediaType mediaType, int tmdbId, RelatedKind kind);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertRelated(List<RelatedMediaEntity> related);

    /** Target titles must already be stored in {@code media}. */
    @Transaction
    public void replaceRelated(MediaType mediaType, int tmdbId, RelatedKind kind, List<RelatedMediaEntity> related) {
        deleteRelated(mediaType, tmdbId, kind);
        insertRelated(related);
    }
}

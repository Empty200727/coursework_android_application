package ru.kinopolka.core.database.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.database.entity.MediaGenreEntity;
import ru.kinopolka.core.database.entity.MediaSummaryUpdate;
import ru.kinopolka.core.model.MediaType;

/**
 * Never use {@link OnConflictStrategy#REPLACE} for {@code media}: it deletes the row first and the
 * foreign keys cascade to cast, genres, related titles and feeds.
 */
@Dao
public abstract class MediaDao {

    @Query("SELECT * FROM media WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract LiveData<MediaEntity> observe(MediaType mediaType, int tmdbId);

    @Nullable
    @Query("SELECT * FROM media WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract MediaEntity get(MediaType mediaType, int tmdbId);

    /** Writes full rows, e.g. from the title card. */
    @Upsert
    public abstract void upsert(List<MediaEntity> media);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract void insertIgnoringExisting(List<MediaEntity> media);

    @Update(entity = MediaEntity.class)
    protected abstract void updateSummaries(List<MediaSummaryUpdate> summaries);

    /**
     * Writes rows from list responses: new titles are inserted, known titles get only the list
     * columns updated, so previously loaded details survive.
     */
    @Transaction
    public void upsertSummaries(List<MediaEntity> media) {
        insertIgnoringExisting(media);
        List<MediaSummaryUpdate> summaries = new ArrayList<>(media.size());
        for (MediaEntity entity : media) {
            summaries.add(entity.toSummaryUpdate());
        }
        updateSummaries(summaries);
    }

    @Query("SELECT genre_id FROM media_genre WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract LiveData<List<Integer>> observeGenreIds(MediaType mediaType, int tmdbId);

    @Query("SELECT genre_id FROM media_genre WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract List<Integer> getGenreIds(MediaType mediaType, int tmdbId);

    @Query("DELETE FROM media_genre WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    protected abstract void deleteGenres(MediaType mediaType, int tmdbId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract void insertGenres(List<MediaGenreEntity> genres);

    @Transaction
    public void replaceGenres(MediaType mediaType, int tmdbId, List<Integer> genreIds) {
        deleteGenres(mediaType, tmdbId);
        List<MediaGenreEntity> rows = new ArrayList<>();
        for (Integer genreId : new LinkedHashSet<>(genreIds)) {
            rows.add(new MediaGenreEntity(mediaType, tmdbId, genreId));
        }
        insertGenres(rows);
    }

    /**
     * Cache cleanup: removes titles cached before {@code threshold} that are not in the library.
     * Cast, genres, related titles and feed items go away by cascade.
     */
    @Query("DELETE FROM media WHERE cached_at < :threshold AND NOT EXISTS ("
            + " SELECT 1 FROM library_entry"
            + " WHERE library_entry.media_type = media.media_type AND library_entry.tmdb_id = media.tmdb_id)")
    public abstract int deleteUnusedCachedBefore(long threshold);

    /** Titles of the user's library; they are never removed by the cache cleanup. */
    @Query("SELECT * FROM media WHERE EXISTS ("
            + " SELECT 1 FROM library_entry"
            + " WHERE library_entry.media_type = media.media_type AND library_entry.tmdb_id = media.tmdb_id)")
    public abstract LiveData<List<MediaEntity>> observeLibraryMedia();

    @Query("SELECT COUNT(*) FROM media")
    public abstract int count();
}

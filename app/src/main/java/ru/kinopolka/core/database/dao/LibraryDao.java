package ru.kinopolka.core.database.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;
import java.util.List;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;

@Dao
public abstract class LibraryDao {

    @Query("SELECT * FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract LiveData<LibraryEntryEntity> observeEntry(MediaType mediaType, int tmdbId);

    @Nullable
    @Query("SELECT * FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract LibraryEntryEntity get(MediaType mediaType, int tmdbId);

    @Query("SELECT * FROM library_entry ORDER BY added_at DESC")
    public abstract LiveData<List<LibraryEntryEntity>> observeAll();

    @Query("SELECT * FROM library_entry ORDER BY added_at DESC")
    public abstract List<LibraryEntryEntity> getAll();

    @Query("SELECT * FROM library_entry WHERE watch_status = :status ORDER BY added_at DESC")
    public abstract List<LibraryEntryEntity> getByStatus(WatchStatus status);

    @Query("SELECT * FROM library_entry WHERE is_favorite = 1 ORDER BY added_at DESC")
    public abstract List<LibraryEntryEntity> getFavorites();

    @Upsert
    public abstract void upsert(LibraryEntryEntity entry);

    @Query("DELETE FROM library_entry WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract void delete(MediaType mediaType, int tmdbId);

    @Query("UPDATE library_entry SET local_poster_path = :path WHERE media_type = :mediaType AND tmdb_id = :tmdbId")
    public abstract void updatePosterPath(MediaType mediaType, int tmdbId, @Nullable String path);

    /** Posters saved for the library (N-01); files not listed here can be deleted. */
    @Query("SELECT local_poster_path FROM library_entry WHERE local_poster_path IS NOT NULL")
    public abstract List<String> posterPaths();
}

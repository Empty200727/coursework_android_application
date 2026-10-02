package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import ru.kinopolka.core.model.MediaType;

@Entity(
        tableName = "cast_member",
        primaryKeys = {"media_type", "tmdb_id", "person_id"},
        foreignKeys = @ForeignKey(
                entity = MediaEntity.class,
                parentColumns = {"media_type", "tmdb_id"},
                childColumns = {"media_type", "tmdb_id"},
                onDelete = ForeignKey.CASCADE))
public class CastMemberEntity {
    @ColumnInfo(name = "media_type") @NonNull public final MediaType mediaType;
    @ColumnInfo(name = "tmdb_id") public final int tmdbId;
    @ColumnInfo(name = "person_id") public final int personId;
    @ColumnInfo(name = "name") @NonNull public final String name;
    @ColumnInfo(name = "character") @Nullable public final String character;
    @ColumnInfo(name = "profile_path") @Nullable public final String profilePath;
    @ColumnInfo(name = "cast_order") public final int castOrder;

    public CastMemberEntity(@NonNull MediaType mediaType, int tmdbId, int personId, @NonNull String name,
            @Nullable String character, @Nullable String profilePath, int castOrder) {
        this.mediaType = mediaType;
        this.tmdbId = tmdbId;
        this.personId = personId;
        this.name = name;
        this.character = character;
        this.profilePath = profilePath;
        this.castOrder = castOrder;
    }
}

package ru.kinopolka.core.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Search history (F-16, optional feature); the table is part of schema version 1. */
@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey @ColumnInfo(name = "query") @NonNull public final String query;
    @ColumnInfo(name = "searched_at") public final long searchedAt;

    public SearchHistoryEntity(@NonNull String query, long searchedAt) {
        this.query = query;
        this.searchedAt = searchedAt;
    }
}

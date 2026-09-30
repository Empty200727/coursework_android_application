package ru.kinopolka.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Search history (F-16, optional feature); the table is part of schema version 1. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "query") val query: String,
    @ColumnInfo(name = "searched_at") val searchedAt: Long,
)

package com.example.aioapp.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["title"]),
        Index(value = ["createdAt"]),
        Index(value = ["modifiedAt"])
    ]
)
data class Note(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val title: String,
    val content: String,
    val color: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

enum class NoteSortOrder {
    ALPHABETICAL,
    CREATION_DATE,
    MODIFICATION_DATE
}

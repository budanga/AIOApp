package com.example.aioapp.core.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Date

data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
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

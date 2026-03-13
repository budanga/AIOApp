package com.example.aioapp.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "truco_games")
data class TrucoGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nosPoints: Int,
    val ellosPoints: Int,
    val maxPoints: Int,
    val winner: String,
    val timestamp: Long = System.currentTimeMillis()
)

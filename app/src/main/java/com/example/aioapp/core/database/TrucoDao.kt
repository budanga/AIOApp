package com.example.aioapp.core.database

import androidx.room.*
import com.example.aioapp.core.model.TrucoGame
import kotlinx.coroutines.flow.Flow

@Dao
interface TrucoDao {
    @Query("SELECT * FROM truco_games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<TrucoGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: TrucoGame)

    @Query("DELETE FROM truco_games")
    suspend fun clearHistory()
}

package com.example.aioapp.core.repository

import com.example.aioapp.core.database.TrucoDao
import com.example.aioapp.core.model.TrucoGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface TrucoRepository {
    fun getAllGames(): Flow<List<TrucoGame>>
    suspend fun insertGame(game: TrucoGame)
    suspend fun clearHistory()
}

@Singleton
class TrucoRepositoryImpl @Inject constructor(
    private val trucoDao: TrucoDao
) : TrucoRepository {
    override fun getAllGames(): Flow<List<TrucoGame>> = trucoDao.getAllGames()

    override suspend fun insertGame(game: TrucoGame) = withContext(Dispatchers.IO) {
        trucoDao.insertGame(game)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        trucoDao.clearHistory()
    }
}

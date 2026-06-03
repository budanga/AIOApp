package com.example.aioapp.ui.minesweeper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.minesweeperDataStore: DataStore<Preferences> by preferencesDataStore(name = "minesweeper_prefs")

data class SavedGame(
    val cells: List<Cell>,
    val difficulty: Difficulty,
    val timeElapsed: Int,
    val minesRemaining: Int,
    val gameState: GameState,
    val isFirstClick: Boolean
)

@Singleton
class MinesweeperRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val GSON = Gson()
    private val SAVED_GAME_KEY = stringPreferencesKey("saved_game")
    private val VIBRATIONS_ENABLED_KEY = booleanPreferencesKey("vibrations_enabled")

    val savedGameFlow: Flow<SavedGame?> = context.minesweeperDataStore.data.map { preferences ->
        val json = preferences[SAVED_GAME_KEY]
        if (json != null) {
            try {
                GSON.fromJson(json, SavedGame::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveGame(game: SavedGame) {
        val json = GSON.toJson(game)
        context.minesweeperDataStore.edit { preferences ->
            preferences[SAVED_GAME_KEY] = json
        }
    }

    suspend fun clearSavedGame() {
        context.minesweeperDataStore.edit { preferences ->
            preferences.remove(SAVED_GAME_KEY)
        }
    }

    val vibrationsEnabledFlow: Flow<Boolean> = context.minesweeperDataStore.data.map { preferences ->
        preferences[VIBRATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setVibrationsEnabled(enabled: Boolean) {
        context.minesweeperDataStore.edit { preferences ->
            preferences[VIBRATIONS_ENABLED_KEY] = enabled
        }
    }
}

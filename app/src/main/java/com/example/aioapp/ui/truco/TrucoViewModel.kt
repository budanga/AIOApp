package com.example.aioapp.ui.truco

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.TrucoGame
import com.example.aioapp.core.repository.TrucoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private val Context.trucoDataStore by preferencesDataStore("truco_prefs")
private val NOS_POINTS_15_KEY = intPreferencesKey("nos_points_15")
private val ELLOS_POINTS_15_KEY = intPreferencesKey("ellos_points_15")
private val NOS_POINTS_30_KEY = intPreferencesKey("nos_points_30")
private val ELLOS_POINTS_30_KEY = intPreferencesKey("ellos_points_30")
private val MAX_POINTS_KEY = intPreferencesKey("max_points")

data class TrucoUiState(
    val nosPoints15: Int = 0,
    val ellosPoints15: Int = 0,
    val nosPoints30: Int = 0,
    val ellosPoints30: Int = 0,
    val maxPoints: Int = 30,
    val isGameOver: Boolean = false,
    val winner: String? = null,
    val history: List<TrucoGame> = emptyList()
) {
    val nosPoints: Int get() = if (maxPoints == 15) nosPoints15 else nosPoints30
    val ellosPoints: Int get() = if (maxPoints == 15) ellosPoints15 else ellosPoints30
}

@HiltViewModel
class TrucoViewModel @Inject constructor(
    application: Application,
    private val trucoRepository: TrucoRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TrucoUiState())
    val uiState: StateFlow<TrucoUiState> = _uiState.asStateFlow()

    init {
        // Load persistable state
        viewModelScope.launch {
            getApplication<Application>().trucoDataStore.data
                .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
                .collect { prefs ->
                    _uiState.update { it.copy(
                        nosPoints15 = prefs[NOS_POINTS_15_KEY] ?: 0,
                        ellosPoints15 = prefs[ELLOS_POINTS_15_KEY] ?: 0,
                        nosPoints30 = prefs[NOS_POINTS_30_KEY] ?: 0,
                        ellosPoints30 = prefs[ELLOS_POINTS_30_KEY] ?: 0,
                        maxPoints = prefs[MAX_POINTS_KEY] ?: 30
                    ) }
                }
        }

        // Load history
        viewModelScope.launch {
            trucoRepository.getAllGames().collect { games ->
                _uiState.update { it.copy(history = games) }
            }
        }
    }

    private fun persistState() {
        val current = _uiState.value
        viewModelScope.launch {
            getApplication<Application>().trucoDataStore.edit { prefs ->
                prefs[NOS_POINTS_15_KEY] = current.nosPoints15
                prefs[ELLOS_POINTS_15_KEY] = current.ellosPoints15
                prefs[NOS_POINTS_30_KEY] = current.nosPoints30
                prefs[ELLOS_POINTS_30_KEY] = current.ellosPoints30
                prefs[MAX_POINTS_KEY] = current.maxPoints
            }
        }
    }

    fun incrementNos() {
        if (_uiState.value.isGameOver) return
        _uiState.update { state ->
            if (state.maxPoints == 15) {
                val next = (state.nosPoints15 + 1).coerceAtMost(15)
                val isOver = next >= 15
                if (isOver) saveGame(next, state.ellosPoints15, "Nos", 15)
                state.copy(nosPoints15 = next, isGameOver = isOver, winner = if (isOver) "Nos" else null)
            } else {
                val next = (state.nosPoints30 + 1).coerceAtMost(30)
                val isOver = next >= 30
                if (isOver) saveGame(next, state.ellosPoints30, "Nos", 30)
                state.copy(nosPoints30 = next, isGameOver = isOver, winner = if (isOver) "Nos" else null)
            }
        }
        persistState()
    }

    fun decrementNos() {
        if (_uiState.value.isGameOver) return
        _uiState.update { state ->
            if (state.maxPoints == 15) {
                state.copy(nosPoints15 = (state.nosPoints15 - 1).coerceAtLeast(0))
            } else {
                state.copy(nosPoints30 = (state.nosPoints30 - 1).coerceAtLeast(0))
            }
        }
        persistState()
    }

    fun incrementEllos() {
        if (_uiState.value.isGameOver) return
        _uiState.update { state ->
            if (state.maxPoints == 15) {
                val next = (state.ellosPoints15 + 1).coerceAtMost(15)
                val isOver = next >= 15
                if (isOver) saveGame(state.nosPoints15, next, "Ellos", 15)
                state.copy(ellosPoints15 = next, isGameOver = isOver, winner = if (isOver) "Ellos" else null)
            } else {
                val next = (state.ellosPoints30 + 1).coerceAtMost(30)
                val isOver = next >= 30
                if (isOver) saveGame(state.nosPoints30, next, "Ellos", 30)
                state.copy(ellosPoints30 = next, isGameOver = isOver, winner = if (isOver) "Ellos" else null)
            }
        }
        persistState()
    }

    fun decrementEllos() {
        if (_uiState.value.isGameOver) return
        _uiState.update { state ->
            if (state.maxPoints == 15) {
                state.copy(ellosPoints15 = (state.ellosPoints15 - 1).coerceAtLeast(0))
            } else {
                state.copy(ellosPoints30 = (state.ellosPoints30 - 1).coerceAtLeast(0))
            }
        }
        persistState()
    }

    private fun saveGame(nos: Int, ellos: Int, winner: String, max: Int) {
        viewModelScope.launch {
            trucoRepository.insertGame(
                TrucoGame(
                    nosPoints = nos,
                    ellosPoints = ellos,
                    winner = winner,
                    maxPoints = max
                )
            )
        }
    }

    fun setMaxPoints(points: Int) {
        _uiState.update { state ->
            // Check if the game being switched TO is already over
            val nos = if (points == 15) state.nosPoints15 else state.nosPoints30
            val ellos = if (points == 15) state.ellosPoints15 else state.ellosPoints30
            val isOver = nos >= points || ellos >= points

            state.copy(
                maxPoints = points,
                isGameOver = isOver,
                winner = when {
                    nos >= points -> "Nos"
                    ellos >= points -> "Ellos"
                    else -> null
                }
            )
        }
        persistState()
    }

    fun reset() {
        _uiState.update { state ->
            if (state.maxPoints == 15) {
                state.copy(nosPoints15 = 0, ellosPoints15 = 0, isGameOver = false, winner = null)
            } else {
                state.copy(nosPoints30 = 0, ellosPoints30 = 0, isGameOver = false, winner = null)
            }
        }
        persistState()
    }

    fun clearHistory() {
        viewModelScope.launch {
            trucoRepository.clearHistory()
        }
    }
}

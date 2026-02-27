package com.example.aioapp.ui.pomodoro

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.PomodoroMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PomodoroUiState(
    val timeLeft: Long = PomodoroMode.WORK.timeMinutes * 60,
    val isRunning: Boolean = false,
    val currentMode: PomodoroMode = PomodoroMode.WORK,
    val progress: Float = 1f
)

@HiltViewModel
class PomodoroViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun start() {
        if (_uiState.value.isRunning) return
        
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0) {
                delay(1000)
                _uiState.update { state ->
                    val newTime = state.timeLeft - 1
                    val totalSeconds = state.currentMode.timeMinutes * 60
                    state.copy(
                        timeLeft = newTime,
                        progress = if (totalSeconds > 0) newTime.toFloat() / totalSeconds else 0f
                    )
                }
            }
            _uiState.update { it.copy(isRunning = false, progress = 0f) }
            triggerNotification()
        }
    }

    fun pause() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun reset() {
        timerJob?.cancel()
        val totalSeconds = _uiState.value.currentMode.timeMinutes * 60
        _uiState.update { 
            it.copy(
                timeLeft = totalSeconds,
                isRunning = false,
                progress = 1f
            )
        }
    }

    fun setMode(mode: PomodoroMode) {
        timerJob?.cancel()
        val totalSeconds = mode.timeMinutes * 60
        _uiState.update { 
            it.copy(
                currentMode = mode,
                timeLeft = totalSeconds,
                isRunning = false,
                progress = 1f
            )
        }
    }

    private fun triggerNotification() {
        // Placeholder for notification implementation
        Log.d("PomodoroViewModel", "Timer finished! Notification triggered.")
        // In a real app, use NotificationManager to show a notification
    }
}

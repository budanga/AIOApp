package com.example.aioapp.ui.pomodoro

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.PomodoroMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PomodoroUiState(
    val timeLeft: Long = PomodoroMode.WORK.totalSeconds,
    val isRunning: Boolean = false,
    val currentMode: PomodoroMode = PomodoroMode.WORK,
    val progress: Float = 1f
)

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var pomodoroService: PomodoroService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PomodoroService.LocalBinder
            pomodoroService = binder.getService()
            isBound = true
            observeServiceState()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            pomodoroService = null
            stateJob?.cancel()
        }
    }

    private var stateJob: Job? = null

    init {
        Intent(application, PomodoroService::class.java).also { intent ->
            application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        // A reconnection gets a new service instance: drop the collector of the old one
        stateJob?.cancel()
        val state = pomodoroService?.uiState ?: return
        stateJob = viewModelScope.launch {
            state.collect { _uiState.value = it }
        }
    }

    fun start() {
        // Started (not only bound) so the timer survives leaving the screen; the intent runs startTimer()
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    // Each action goes through one path only: the bound service directly, or an intent if not bound yet
    fun pause() {
        pomodoroService?.pauseTimer() ?: sendAction(PomodoroService.ACTION_PAUSE)
    }

    fun reset() {
        pomodoroService?.resetTimer() ?: sendAction(PomodoroService.ACTION_RESET)
    }

    fun setMode(mode: PomodoroMode) {
        pomodoroService?.setMode(mode) ?: sendAction(PomodoroService.ACTION_SET_MODE) {
            putExtra(PomodoroService.EXTRA_MODE, mode.name)
        }
    }

    private fun sendAction(action: String, extras: Intent.() -> Unit = {}) {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            this.action = action
            extras()
        }
        getApplication<Application>().startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}

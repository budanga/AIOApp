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
import kotlinx.coroutines.flow.collectLatest
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
        }
    }

    init {
        Intent(application, PomodoroService::class.java).also { intent ->
            application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            pomodoroService?.uiState?.collectLatest { serviceState ->
                _uiState.value = serviceState
            }
        }
    }

    fun start() {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
        pomodoroService?.startTimer()
    }

    fun pause() {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
        pomodoroService?.pauseTimer()
    }

    fun reset() {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_RESET
        }
        getApplication<Application>().startService(intent)
        pomodoroService?.resetTimer()
    }

    fun setMode(mode: PomodoroMode) {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_SET_MODE
            putExtra(PomodoroService.EXTRA_MODE, mode.name)
        }
        getApplication<Application>().startService(intent)
        pomodoroService?.setMode(mode)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}

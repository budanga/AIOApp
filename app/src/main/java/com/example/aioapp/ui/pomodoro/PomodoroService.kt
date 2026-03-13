package com.example.aioapp.ui.pomodoro

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.aioapp.AIOApplication.Companion.POMODORO_CHANNEL_ID
import com.example.aioapp.MainActivity
import com.example.aioapp.core.model.PomodoroMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class PomodoroService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PomodoroService = this@PomodoroService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
            ACTION_SET_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_MODE)
                val mode = PomodoroMode.entries.find { it.name == modeName } ?: PomodoroMode.WORK
                setMode(mode)
            }
        }
        return START_STICKY
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        _uiState.update { it.copy(isRunning = true) }
        updateNotification()
        
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_uiState.value.timeLeft > 0) {
                delay(1000)
                _uiState.update { state ->
                    val newTime = state.timeLeft - 1
                    val totalSeconds = state.currentMode.totalSeconds
                    state.copy(
                        timeLeft = newTime,
                        progress = if (totalSeconds > 0) newTime.toFloat() / totalSeconds else 0f
                    )
                }
                updateNotification()
            }
            _uiState.update { it.copy(isRunning = false, progress = 0f) }
            updateNotification()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
        updateNotification()
    }

    fun resetTimer() {
        timerJob?.cancel()
        val totalSeconds = _uiState.value.currentMode.totalSeconds
        _uiState.update {
            it.copy(
                timeLeft = totalSeconds,
                isRunning = false,
                progress = 1f
            )
        }
        updateNotification()
    }

    fun setMode(mode: PomodoroMode) {
        timerJob?.cancel()
        val totalSeconds = mode.totalSeconds
        _uiState.update {
            it.copy(
                currentMode = mode,
                timeLeft = totalSeconds,
                isRunning = false,
                progress = 1f
            )
        }
        updateNotification()
    }

    private fun updateNotification() {
        val state = _uiState.value
        
        val minutes = state.timeLeft / 60
        val seconds = state.timeLeft % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val modeNameFormatted = state.currentMode.name.replace("_", " ")
            .lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val contentTitle = if (state.currentMode == PomodoroMode.WORK) "Working" else modeNameFormatted
        val contentText = "$timeStr remaining"

        // The Intent that will be used to open the Pomodoro screen
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_NAVIGATE_POMODORO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Use FLAG_UPDATE_CURRENT to ensure the action is updated in the existing PendingIntent
        val mainPendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            mainIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseStartAction = if (state.isRunning) {
            val pauseIntent = Intent(this, PomodoroService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Pause", pausePendingIntent).build()
        } else {
            val startIntent = Intent(this, PomodoroService::class.java).apply { action = ACTION_START }
            val startPendingIntent = PendingIntent.getService(this, 2, startIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Start", startPendingIntent).build()
        }

        val resetIntent = Intent(this, PomodoroService::class.java).apply { action = ACTION_RESET }
        val resetPendingIntent = PendingIntent.getService(this, 3, resetIntent, PendingIntent.FLAG_IMMUTABLE)
        val resetAction = NotificationCompat.Action.Builder(0, "Reset", resetPendingIntent).build()

        val notification = NotificationCompat.Builder(this, POMODORO_CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(mainPendingIntent)
            .setOngoing(state.isRunning)
            .addAction(pauseStartAction)
            .addAction(resetAction)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.aioapp.ACTION_START"
        const val ACTION_PAUSE = "com.example.aioapp.ACTION_PAUSE"
        const val ACTION_RESET = "com.example.aioapp.ACTION_RESET"
        const val ACTION_SET_MODE = "com.example.aioapp.ACTION_SET_MODE"
        const val EXTRA_MODE = "EXTRA_MODE"
    }
}

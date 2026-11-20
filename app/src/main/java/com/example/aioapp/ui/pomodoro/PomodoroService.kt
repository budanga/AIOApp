package com.example.aioapp.ui.pomodoro

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.aioapp.AIOApplication.Companion.POMODORO_ALERT_CHANNEL_ID
import com.example.aioapp.AIOApplication.Companion.POMODORO_CHANNEL_ID
import com.example.aioapp.MainActivity
import com.example.aioapp.R
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
    private var endAtElapsed = 0L
    private var wakeLock: PowerManager.WakeLock? = null

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
        val timeLeft = _uiState.value.timeLeft
        if (_uiState.value.isRunning || timeLeft <= 0) return

        // Count against the wall clock so ticks delayed by doze or slow frames don't add drift.
        endAtElapsed = SystemClock.elapsedRealtime() + timeLeft * 1000
        acquireWakeLock(timeLeft * 1000 + WAKE_LOCK_SLACK_MS)
        _uiState.update { it.copy(isRunning = true) }
        updateNotification()

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val remaining = secondsRemaining(endAtElapsed, now)
                _uiState.update { state ->
                    val totalSeconds = state.currentMode.totalSeconds
                    state.copy(
                        timeLeft = remaining,
                        progress = if (totalSeconds > 0) remaining.toFloat() / totalSeconds else 0f
                    )
                }
                if (remaining <= 0) break
                // No notification update here: while running it shows a system-driven countdown chronometer
                delay(millisUntilNextTick(endAtElapsed, now))
            }
            _uiState.update { it.copy(isRunning = false, progress = 0f) }
            releaseWakeLock()
            updateNotification()
            notifySessionFinished()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        releaseWakeLock()
        _uiState.update { state ->
            if (!state.isRunning) state
            else state.copy(isRunning = false, timeLeft = secondsRemaining(endAtElapsed, SystemClock.elapsedRealtime()))
        }
        updateNotification()
    }

    fun resetTimer() {
        timerJob?.cancel()
        releaseWakeLock()
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
        releaseWakeLock()
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
        // While running the countdown chronometer shows the time, so the text doesn't need per-second rebuilds
        val contentText = if (state.isRunning) null else "$timeStr remaining"
        val remainingMs = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)

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
            .setUsesChronometer(state.isRunning)
            .setChronometerCountDown(state.isRunning)
            .setShowWhen(state.isRunning)
            .setWhen(if (state.isRunning) System.currentTimeMillis() + remainingMs else 0L)
            .addAction(pauseStartAction)
            .addAction(resetAction)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun notifySessionFinished() {
        val modeLabel = getString(
            when (_uiState.value.currentMode) {
                PomodoroMode.WORK -> R.string.pomodoro_mode_work
                PomodoroMode.SHORT_BREAK -> R.string.pomodoro_mode_short_break
                PomodoroMode.LONG_BREAK -> R.string.pomodoro_mode_long_break
            }
        )
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_NAVIGATE_POMODORO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notification = NotificationCompat.Builder(this, POMODORO_ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.pomodoro_finished_title))
            .setContentText(getString(R.string.pomodoro_finished_text, modeLabel))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(
                PendingIntent.getActivity(this, 4, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(FINISHED_NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIOApp:Pomodoro")
            .apply { acquire(timeoutMs) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        releaseWakeLock()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val FINISHED_NOTIFICATION_ID = 1002
        private const val WAKE_LOCK_SLACK_MS = 10_000L

        /** Whole seconds left until [endAt], rounded up so "00:00" only shows at the end. */
        fun secondsRemaining(endAt: Long, now: Long): Long =
            if (now >= endAt) 0 else (endAt - now + 999) / 1000

        /** Delay until the displayed second next changes. */
        fun millisUntilNextTick(endAt: Long, now: Long): Long {
            val rest = (endAt - now) % 1000
            return if (rest <= 0) 1000 else rest
        }
        const val ACTION_START = "com.example.aioapp.ACTION_START"
        const val ACTION_PAUSE = "com.example.aioapp.ACTION_PAUSE"
        const val ACTION_RESET = "com.example.aioapp.ACTION_RESET"
        const val ACTION_SET_MODE = "com.example.aioapp.ACTION_SET_MODE"
        const val EXTRA_MODE = "EXTRA_MODE"
    }
}

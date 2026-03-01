package com.example.aioapp.core.model

enum class PomodoroMode(val timeMinutes: Long, val timeSeconds: Long = 0) {
    WORK(25),
    LONG_BREAK(15),
    SHORT_BREAK(5);

    val totalSeconds: Long
        get() = timeMinutes * 60 + timeSeconds
}

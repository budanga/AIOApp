package com.example.aioapp.core.model

enum class PomodoroMode(val timeMinutes: Long) {
    WORK(25),
    SHORT_BREAK(5),
    LONG_BREAK(15)
}

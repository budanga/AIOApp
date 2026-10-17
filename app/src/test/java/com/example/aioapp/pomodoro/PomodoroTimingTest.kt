package com.example.aioapp.pomodoro

import com.example.aioapp.ui.pomodoro.PomodoroService.Companion.millisUntilNextTick
import com.example.aioapp.ui.pomodoro.PomodoroService.Companion.secondsRemaining
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroTimingTest {

    private val endAt = 1_500_000L

    @Test
    fun `remaining seconds are rounded up`() {
        assertEquals(1500L, secondsRemaining(endAt, 0))
        assertEquals(1500L, secondsRemaining(endAt, 1))
        assertEquals(1L, secondsRemaining(endAt, endAt - 1))
    }

    @Test
    fun `remaining is zero at or after the end`() {
        assertEquals(0L, secondsRemaining(endAt, endAt))
        assertEquals(0L, secondsRemaining(endAt, endAt + 5_000))
    }

    @Test
    fun `late ticks do not accumulate drift`() {
        // Device slept for 7.3 s: the countdown jumps straight to the real remaining time
        assertEquals(1493L, secondsRemaining(endAt, 7_300))
    }

    @Test
    fun `next tick waits until the displayed second changes`() {
        assertEquals(1000L, millisUntilNextTick(endAt, 0))
        assertEquals(300L, millisUntilNextTick(endAt, 7_700))
        assertEquals(1L, millisUntilNextTick(endAt, endAt - 1))
    }
}

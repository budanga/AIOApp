package com.example.aioapp.truco

import com.example.aioapp.ui.truco.TrucoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrucoUiStateTest {

    @Test
    fun `restored finished game is marked as over`() {
        // State as loaded from DataStore: points restored, isGameOver defaults to false
        val restored = TrucoUiState(nosPoints30 = 30, ellosPoints30 = 12, maxPoints = 30)

        val state = restored.withGameStatus()

        assertTrue(state.isGameOver)
        assertEquals("Nos", state.winner)
    }

    @Test
    fun `game in progress is not over`() {
        val state = TrucoUiState(nosPoints30 = 29, ellosPoints30 = 29, maxPoints = 30).withGameStatus()

        assertFalse(state.isGameOver)
        assertNull(state.winner)
    }

    @Test
    fun `status uses points of the active mode only`() {
        val base = TrucoUiState(nosPoints15 = 3, ellosPoints15 = 15, nosPoints30 = 10, ellosPoints30 = 4)

        val in15 = base.copy(maxPoints = 15).withGameStatus()
        val in30 = base.copy(maxPoints = 30).withGameStatus()

        assertTrue(in15.isGameOver)
        assertEquals("Ellos", in15.winner)
        assertFalse(in30.isGameOver)
        assertNull(in30.winner)
    }
}

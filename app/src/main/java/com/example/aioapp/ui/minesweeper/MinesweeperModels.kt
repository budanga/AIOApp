package com.example.aioapp.ui.minesweeper

data class Cell(
    val x: Int,
    val y: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val surroundingMines: Int = 0
)

enum class GameState {
    Idle, Playing, Paused, Won, Lost
}

data class Difficulty(
    val nameResId: Int,
    val columns: Int,
    val rows: Int,
    val mines: Int,
    val isCustom: Boolean = false
)

val BeginnerDifficulty = Difficulty(com.example.aioapp.R.string.ms_beginner, 8, 10, 10)
val EasyDifficulty = Difficulty(com.example.aioapp.R.string.ms_easy, 10, 12, 15)
val IntermediateDifficulty = Difficulty(com.example.aioapp.R.string.ms_intermediate, 14, 18, 40)
val AdvancedDifficulty = Difficulty(com.example.aioapp.R.string.ms_advanced, 18, 24, 99)

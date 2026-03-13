package com.example.aioapp.ui.minesweeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class MinesweeperViewModel @Inject constructor(
    private val repository: MinesweeperRepository
) : ViewModel() {
    private val _gameState = MutableStateFlow(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _cells = MutableStateFlow<List<Cell>>(emptyList())
    val cells: StateFlow<List<Cell>> = _cells.asStateFlow()

    private val _difficulty = MutableStateFlow<Difficulty?>(null)
    val difficulty: StateFlow<Difficulty?> = _difficulty.asStateFlow()
    
    private val _timeElapsed = MutableStateFlow(0)
    val timeElapsed: StateFlow<Int> = _timeElapsed.asStateFlow()
    
    private val _minesRemaining = MutableStateFlow(0)
    val minesRemaining: StateFlow<Int> = _minesRemaining.asStateFlow()

    private val _isFlagMode = MutableStateFlow(false)
    val isFlagMode: StateFlow<Boolean> = _isFlagMode.asStateFlow()

    private var isFirstClick = true
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.savedGameFlow.collect { saved ->
                // Only load if we are currently Idle and there's a valid saved game
                if (saved != null && _gameState.value == GameState.Idle) {
                    _difficulty.value = saved.difficulty
                    _cells.value = saved.cells
                    _timeElapsed.value = saved.timeElapsed
                    _minesRemaining.value = saved.minesRemaining
                    // If it was won or lost, keep that state but don't resume timer
                    _gameState.value = if (saved.gameState == GameState.Playing) GameState.Paused else saved.gameState
                    isFirstClick = saved.isFirstClick
                }
            }
        }
    }

    private fun saveCurrentGame() {
        val diff = _difficulty.value ?: return
        viewModelScope.launch {
            repository.saveGame(
                SavedGame(
                    cells = _cells.value,
                    difficulty = diff,
                    timeElapsed = _timeElapsed.value,
                    minesRemaining = _minesRemaining.value,
                    gameState = _gameState.value,
                    isFirstClick = isFirstClick
                )
            )
        }
    }

    fun startNewGame(selectedDifficulty: Difficulty) {
       _difficulty.value = selectedDifficulty
       _gameState.value = GameState.Playing
       _timeElapsed.value = 0
       _minesRemaining.value = selectedDifficulty.mines
       _isFlagMode.value = false
       isFirstClick = true
       timerJob?.cancel()
       
       val initialCells = buildList {
           for (y in 0 until selectedDifficulty.rows) {
               for (x in 0 until selectedDifficulty.columns) {
                   add(Cell(x = x, y = y))
               }
           }
       }
       _cells.value = initialCells
       saveCurrentGame()
    }

    fun onCellClicked(x: Int, y: Int) {
       if (_gameState.value != GameState.Playing) return
       
       if (_isFlagMode.value) {
            onCellLongClicked(x, y)
            return
       }
       
       val diff = _difficulty.value ?: return
       val index = y * diff.columns + x
       val currentCells = _cells.value
       val cell = currentCells.getOrNull(index) ?: return
       
       if (cell.isFlagged || cell.isRevealed) return
       
       if (isFirstClick) {
           isFirstClick = false
           generateBoardAndMines(x, y, diff)
           startTimer()
       }

       val newCells = _cells.value.toMutableList()
       
       if (newCells[index].isMine) {
           revealAllMines(newCells)
           _cells.value = newCells
           _gameState.value = GameState.Lost
           timerJob?.cancel()
       } else {
           floodFill(x, y, newCells, diff)
           _cells.value = newCells
           checkWinCondition()
       }
       saveCurrentGame()
    }
    
    fun onCellLongClicked(x: Int, y: Int) {
       if (_gameState.value != GameState.Playing) return
       val diff = _difficulty.value ?: return
       val index = y * diff.columns + x
       val currentCells = _cells.value.toMutableList()
       val cell = currentCells.getOrNull(index) ?: return
       
       if (cell.isRevealed) return
       
       val newFlaggedState = !cell.isFlagged
       currentCells[index] = cell.copy(isFlagged = newFlaggedState)
       _cells.value = currentCells
       
       val currentMines = _minesRemaining.value
       _minesRemaining.value = if (newFlaggedState) currentMines - 1 else currentMines + 1
       saveCurrentGame()
    }

    private fun generateBoardAndMines(firstMoveX: Int, firstMoveY: Int, diff: Difficulty) {
        val totalCells = diff.columns * diff.rows
        val newCells = _cells.value.toMutableList()
        val validPositions = (0 until totalCells).filter {
            val cx = it % diff.columns
            val cy = it / diff.columns
            abs(cx - firstMoveX) > 1 || abs(cy - firstMoveY) > 1
        }.shuffled().take(diff.mines)
        
        validPositions.forEach { pos ->
            newCells[pos] = newCells[pos].copy(isMine = true)
        }
        
        for (y in 0 until diff.rows) {
            for (x in 0 until diff.columns) {
                val index = y * diff.columns + x
                if (!newCells[index].isMine) {
                    var count = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until diff.columns && ny in 0 until diff.rows) {
                                val nIndex = ny * diff.columns + nx
                                if (newCells[nIndex].isMine) count++
                            }
                        }
                    }
                    newCells[index] = newCells[index].copy(surroundingMines = count)
                }
            }
        }
        _cells.value = newCells
    }

    private fun floodFill(startX: Int, startY: Int, cells: MutableList<Cell>, diff: Difficulty) {
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))
        
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            val index = y * diff.columns + x
            val cell = cells[index]
            
            if (cell.isRevealed || cell.isFlagged) continue
            
            cells[index] = cell.copy(isRevealed = true)
            
            if (cell.surroundingMines == 0) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until diff.columns && ny in 0 until diff.rows) {
                            val nIndex = ny * diff.columns + nx
                            if (!cells[nIndex].isRevealed && !cells[nIndex].isFlagged && !cells[nIndex].isMine) {
                                queue.add(Pair(nx, ny))
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun revealAllMines(cells: MutableList<Cell>) {
        for (i in cells.indices) {
            if (cells[i].isMine) {
                cells[i] = cells[i].copy(isRevealed = true)
            }
        }
    }
    
    private fun checkWinCondition() {
        val currentCells = _cells.value
        val hasUnrevealedSafeCell = currentCells.any { !it.isMine && !it.isRevealed }
        if (!hasUnrevealedSafeCell) {
            _gameState.value = GameState.Won
            timerJob?.cancel()
            _cells.value = currentCells.map { if (it.isMine) it.copy(isFlagged = true) else it }
            _minesRemaining.value = 0
        }
    }
    
    fun toggleFlagMode() {
        _isFlagMode.value = !_isFlagMode.value
    }

    fun pauseGame() {
        if (_gameState.value == GameState.Playing) {
            _gameState.value = GameState.Paused
            timerJob?.cancel()
            saveCurrentGame()
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.Paused || (_gameState.value == GameState.Idle && _difficulty.value != null)) {
            _gameState.value = GameState.Playing
            if (!isFirstClick) {
                startTimer()
            }
            saveCurrentGame()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timeElapsed.value++
                saveCurrentGame()
            }
        }
    }

    fun returnToMenu() {
        if (_gameState.value == GameState.Won || _gameState.value == GameState.Lost) {
            viewModelScope.launch { repository.clearSavedGame() }
            _difficulty.value = null
            _gameState.value = GameState.Idle
        } else {
            pauseGame()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

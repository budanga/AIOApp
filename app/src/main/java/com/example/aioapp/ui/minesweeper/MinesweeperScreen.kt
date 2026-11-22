package com.example.aioapp.ui.minesweeper

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon
import com.example.aioapp.ui.theme.LocalDarkTheme
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val VIEW_MENU = "Menu"
private const val VIEW_GAME = "Game"

private val presetDifficulties = listOf(BeginnerDifficulty, EasyDifficulty, IntermediateDifficulty, AdvancedDifficulty)

@Composable
fun MinesweeperScreen(
    viewModel: MinesweeperViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val difficulty by viewModel.difficulty.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var currentView by rememberSaveable { mutableStateOf(VIEW_MENU) }
    var showCustomSheet by rememberSaveable { mutableStateOf(false) }

    fun startGame(selected: Difficulty) {
        viewModel.startNewGame(selected)
        currentView = VIEW_GAME
    }

    fun backToMenu() {
        viewModel.returnToMenu()
        currentView = VIEW_MENU
    }

    // Stop the clock when the app goes to background or another screen covers this one (not on rotation)
    val activity = LocalActivity.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.pauseGame()
    }

    BackHandler(enabled = currentView == VIEW_GAME) { backToMenu() }

    // Auto return to menu if difficulty is cleared (game properly ended when going back from Win/Loss)
    LaunchedEffect(difficulty, gameState) {
        if (difficulty == null && currentView == VIEW_GAME) {
            currentView = VIEW_MENU
        }
    }

    if (currentView == VIEW_GAME && difficulty != null) {
        GameView(viewModel = viewModel, onBackToMenu = ::backToMenu)
    } else {
        Scaffold(
            topBar = {
                AioTopBar(
                    title = { Text(text = stringResource(R.string.feature_minesweeper)) },
                    navigationIcon = { DefaultNavigationIcon(navController, drawerState, scope) }
                )
            }
        ) { padding ->
            MenuView(
                viewModel = viewModel,
                onContinue = {
                    viewModel.resumeGame()
                    currentView = VIEW_GAME
                },
                onStart = ::startGame,
                onCustom = { showCustomSheet = true },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showCustomSheet) {
        CustomDifficultySheet(
            onDismiss = { showCustomSheet = false },
            onStart = {
                showCustomSheet = false
                startGame(it)
            }
        )
    }
}

// ---------- Menu ----------

@Composable
private fun MenuView(
    viewModel: MinesweeperViewModel,
    onContinue: () -> Unit,
    onStart: (Difficulty) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedDifficulty by viewModel.difficulty.collectAsStateWithLifecycle()
    val cells by viewModel.cells.collectAsStateWithLifecycle()
    val seconds by viewModel.timeElapsed.collectAsStateWithLifecycle()
    val minesRemaining by viewModel.minesRemaining.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        savedDifficulty?.let { saved ->
            SectionTitle(stringResource(R.string.ms_section_in_progress))
            Card(
                onClick = onContinue,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniBoard(columns = saved.columns, rows = saved.rows, cells = cells, height = 56.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ms_board_title, stringResource(saved.nameResId), saved.columns, saved.rows),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.ms_in_progress_summary, formatTime(seconds), minesRemaining),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.ms_continue), modifier = Modifier.size(32.dp))
                }
            }
        }

        SectionTitle(stringResource(R.string.ms_new_game))
        presetDifficulties.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                pair.forEach { preset ->
                    DifficultyCard(preset, onClick = { onStart(preset) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Card(
            onClick = onCustom,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ms_custom), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.ms_custom_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MiniBoard(columns = difficulty.columns, rows = difficulty.rows, cells = emptyList(), height = 40.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(difficulty.nameResId), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ms_board_summary, difficulty.columns, difficulty.rows, difficulty.mines),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Small picture of a board: hidden cells in the accent color, revealed ones in the surface color. */
@Composable
private fun MiniBoard(columns: Int, rows: Int, cells: List<Cell>, height: Dp) {
    val hidden = MaterialTheme.colorScheme.primary
    val revealed = MaterialTheme.colorScheme.surface
    val flagged = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = Modifier.height(height).aspectRatio(columns.toFloat() / rows)) {
        val cell = min(size.width / columns, size.height / rows)
        val gap = cell * 0.15f
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                val state = cells.getOrNull(y * columns + x)
                val color = when {
                    state == null -> hidden
                    state.isRevealed -> revealed
                    state.isFlagged -> flagged
                    else -> hidden
                }
                drawRect(color, topLeft = Offset(x * cell, y * cell), size = Size(cell - gap, cell - gap))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDifficultySheet(onDismiss: () -> Unit, onStart: (Difficulty) -> Unit) {
    var width by rememberSaveable { mutableFloatStateOf(10f) }
    var height by rememberSaveable { mutableFloatStateOf(10f) }
    var mines by rememberSaveable { mutableFloatStateOf(15f) }

    val maxMines = ((width * height) * 0.5f).toInt()
    if (mines > maxMines) mines = maxMines.toFloat()
    val density = (mines / (width * height) * 100).toInt()

    // Fully expanded: half-open, the play button would sit under the navigation bar
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.ms_custom), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MiniBoard(columns = width.toInt(), rows = height.toInt(), cells = emptyList(), height = 120.dp)
            }
            Text(
                stringResource(R.string.ms_density, density),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.ms_width, width.toInt()), style = MaterialTheme.typography.labelLarge)
            // Rounded by hand instead of with `steps`, which would draw a dot for every value
            Slider(value = width, onValueChange = { width = it.roundToInt().toFloat() }, valueRange = 5f..30f)

            Text(stringResource(R.string.ms_height, height.toInt()), style = MaterialTheme.typography.labelLarge)
            Slider(value = height, onValueChange = { height = it.roundToInt().toFloat() }, valueRange = 5f..30f)

            Text(stringResource(R.string.ms_mines, mines.toInt()), style = MaterialTheme.typography.labelLarge)
            Slider(value = mines, onValueChange = { mines = it.roundToInt().toFloat() }, valueRange = 1f..maxMines.toFloat().coerceAtLeast(2f))

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onStart(Difficulty(R.string.ms_custom, width.toInt(), height.toInt(), mines.toInt(), isCustom = true)) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.ms_play))
            }
        }
    }
}

// ---------- Game ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameView(viewModel: MinesweeperViewModel, onBackToMenu: () -> Unit) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val cells by viewModel.cells.collectAsStateWithLifecycle()
    val difficulty = viewModel.difficulty.collectAsStateWithLifecycle().value ?: return
    val isFlagMode by viewModel.isFlagMode.collectAsStateWithLifecycle()
    val minesRemaining by viewModel.minesRemaining.collectAsStateWithLifecycle()
    val vibrationsEnabled by viewModel.vibrationsEnabled.collectAsStateWithLifecycle()
    val isFinished = gameState == GameState.Won || gameState == GameState.Lost
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AioTopBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(
                            icon = Icons.Default.Flag,
                            contentDescription = stringResource(R.string.ms_mines_left_desc),
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer
                        ) { Text(minesRemaining.toString(), style = MaterialTheme.typography.titleMedium) }
                        StatChip(
                            icon = Icons.Default.Timer,
                            contentDescription = stringResource(R.string.ms_timer_desc),
                            container = MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = MaterialTheme.colorScheme.onSurface
                        ) { ElapsedTimeText(viewModel.timeElapsed) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    if (!isFinished) {
                        IconButton(onClick = {
                            if (gameState == GameState.Playing) viewModel.pauseGame() else viewModel.resumeGame()
                        }) {
                            Icon(
                                if (gameState == GameState.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = stringResource(if (gameState == GameState.Paused) R.string.ms_resume else R.string.ms_pause_action)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.ms_more_options))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ms_vibration)) },
                                leadingIcon = { Icon(Icons.Default.Vibration, contentDescription = null) },
                                trailingIcon = { Checkbox(checked = vibrationsEnabled, onCheckedChange = null) },
                                onClick = { viewModel.toggleVibrations() }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                InteractiveMinesweeperBoard(
                    cells = cells,
                    difficulty = difficulty,
                    gameState = gameState,
                    onClick = { x, y -> viewModel.onCellClicked(x, y) },
                    onLongClick = { x, y -> viewModel.onCellLongClicked(x, y) },
                    isFlagMode = isFlagMode,
                    vibrationsEnabled = vibrationsEnabled
                )
                if (gameState == GameState.Paused) {
                    PausedCover(onResume = { viewModel.resumeGame() })
                } else if (isFinished) {
                    ResultEffects(hasWon = gameState == GameState.Won)
                }
            }

            if (isFinished) {
                ResultPanel(
                    hasWon = gameState == GameState.Won,
                    timeElapsed = viewModel.timeElapsed,
                    onPlayAgain = { viewModel.startNewGame(difficulty) },
                    onChangeDifficulty = onBackToMenu
                )
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SegmentedButton(
                        selected = !isFlagMode,
                        onClick = { if (isFlagMode) viewModel.toggleFlagMode() },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) { Text(stringResource(R.string.ms_mode_reveal)) }
                    SegmentedButton(
                        selected = isFlagMode,
                        onClick = { if (!isFlagMode) viewModel.toggleFlagMode() },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) { Text(stringResource(R.string.ms_mode_flag)) }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    container: Color,
    content: Color,
    value: @Composable () -> Unit
) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            value()
        }
    }
}

/** Collects the clock itself so the per-second tick recomposes only this text, not the board. */
@Composable
private fun ElapsedTimeText(timeElapsed: StateFlow<Int>) {
    val seconds by timeElapsed.collectAsStateWithLifecycle()
    Text(formatTime(seconds), style = MaterialTheme.typography.titleMedium)
}

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Hides the board completely while paused, so pausing can't be used to study it. */
@Composable
private fun PausedCover(onResume: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = false) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ms_pause), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onResume, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ms_resume))
        }
    }
}

@Composable
private fun ResultPanel(
    hasWon: Boolean,
    timeElapsed: StateFlow<Int>,
    onPlayAgain: () -> Unit,
    onChangeDifficulty: () -> Unit
) {
    val seconds by timeElapsed.collectAsStateWithLifecycle()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(if (hasWon) R.string.ms_won else R.string.ms_lost),
                style = MaterialTheme.typography.headlineSmall,
                color = if (hasWon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.ms_result_time, formatTime(seconds)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onChangeDifficulty, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.ms_change_difficulty))
                }
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.ms_play_again))
                }
            }
        }
    }
}

// ---------- Board ----------

/** Number colors per count (1..8) that stay readable on the revealed cell in each theme. */
private val lightNumberColors = listOf(
    Color(0xFF185FA5), Color(0xFF3B6D11), Color(0xFFA32D2D), Color(0xFF3C3489),
    Color(0xFF993C1D), Color(0xFF0F6E56), Color(0xFF444441), Color(0xFF5F5E5A)
)
private val darkNumberColors = listOf(
    Color(0xFF85B7EB), Color(0xFF97C459), Color(0xFFF09595), Color(0xFFAFA9EC),
    Color(0xFFF0997B), Color(0xFF5DCAA5), Color(0xFFD3D1C7), Color(0xFFB4B2A9)
)

@Composable
fun InteractiveMinesweeperBoard(
    cells: List<Cell>,
    difficulty: Difficulty,
    gameState: GameState,
    onClick: (Int, Int) -> Unit,
    onLongClick: (Int, Int) -> Unit,
    isFlagMode: Boolean = false,
    vibrationsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val isDark = LocalDarkTheme.current

    val hiddenColor = MaterialTheme.colorScheme.primaryContainer
    val revealedColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val mineCellColor = MaterialTheme.colorScheme.errorContainer
    val mineColor = MaterialTheme.colorScheme.onErrorContainer
    val flagColor = MaterialTheme.colorScheme.tertiary
    val flagPainter = rememberVectorPainter(Icons.Filled.Flag)

    // Zoom and pan start fresh for every board size
    var scale by remember(difficulty) { mutableFloatStateOf(1f) }
    var viewOffset by remember(difficulty) { mutableStateOf(Offset.Zero) }

    // Gesture handlers read the latest values instead of restarting on every change
    val currentCells by rememberUpdatedState(cells)
    val currentFlagMode by rememberUpdatedState(isFlagMode)
    val currentVibrations by rememberUpdatedState(vibrationsEnabled)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    fun vibrate() {
        if (currentVibrations) vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // The whole board fits on screen at scale 1; pinch to zoom in from there
        val cellSizeDp = min(maxWidth.value / difficulty.columns, maxHeight.value / difficulty.rows).coerceIn(16f, 48f).dp
        val cellSize = with(density) { cellSizeDp.toPx() }
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()
        val boardWidth = difficulty.columns * cellSize
        val boardHeight = difficulty.rows * cellSize
        val centerOffsetX = (viewportWidth - boardWidth) / 2f
        val centerOffsetY = (viewportHeight - boardHeight) / 2f

        // The numbers 1..8 measured once per cell size and theme instead of on every cell of every frame
        val numberLayouts = remember(textMeasurer, cellSizeDp, isDark) {
            (if (isDark) darkNumberColors else lightNumberColors).mapIndexed { i, color ->
                textMeasurer.measure(
                    text = (i + 1).toString(),
                    style = TextStyle(color = color, fontSize = (cellSizeDp.value * 0.55f).sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(difficulty, cellSize) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        // Keep the board on screen: it can only move as far as its zoomed size exceeds the viewport
                        val limitX = max(0f, (boardWidth * scale - viewportWidth) / 2f)
                        val limitY = max(0f, (boardHeight * scale - viewportHeight) / 2f)
                        viewOffset = Offset(
                            (viewOffset.x + pan.x).coerceIn(-limitX, limitX),
                            (viewOffset.y + pan.y).coerceIn(-limitY, limitY)
                        )
                    }
                }
                // scale and viewOffset are read at tap time, so pan/zoom frames don't restart this detector
                .pointerInput(difficulty, gameState, cellSize) {
                    if (gameState != GameState.Playing) return@pointerInput

                    fun cellAt(pressPos: Offset): Pair<Int, Int>? {
                        val localX = (pressPos.x - size.width / 2f) / scale + size.width / 2f - viewOffset.x / scale - centerOffsetX
                        val localY = (pressPos.y - size.height / 2f) / scale + size.height / 2f - viewOffset.y / scale - centerOffsetY
                        if (localX !in 0f..boardWidth || localY !in 0f..boardHeight) return null
                        val col = (localX / cellSize).toInt()
                        val row = (localY / cellSize).toInt()
                        return if (col in 0 until difficulty.columns && row in 0 until difficulty.rows) col to row else null
                    }

                    fun isHidden(col: Int, row: Int): Boolean {
                        val index = row * difficulty.columns + col
                        return index in currentCells.indices && !currentCells[index].isRevealed
                    }

                    detectTapGestures(
                        onTap = { pressPos ->
                            val (col, row) = cellAt(pressPos) ?: return@detectTapGestures
                            if (currentFlagMode && isHidden(col, row)) vibrate()
                            currentOnClick(col, row)
                        },
                        onLongPress = { pressPos ->
                            val (col, row) = cellAt(pressPos) ?: return@detectTapGestures
                            if (isHidden(col, row)) vibrate()
                            currentOnLongClick(col, row)
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = viewOffset.x
                        translationY = viewOffset.y
                    }
            ) {
                val pad = cellSize * 0.06f
                val tileSize = Size(cellSize - pad * 2, cellSize - pad * 2)
                val corner = CornerRadius(cellSize * 0.18f)
                val iconSize = cellSize * 0.62f
                for (cell in cells) {
                    val cx = centerOffsetX + cell.x * cellSize
                    val cy = centerOffsetY + cell.y * cellSize
                    val center = Offset(cx + cellSize / 2, cy + cellSize / 2)

                    val tileColor = when {
                        !cell.isRevealed -> hiddenColor
                        cell.isMine -> mineCellColor
                        else -> revealedColor
                    }
                    drawRoundRect(tileColor, topLeft = Offset(cx + pad, cy + pad), size = tileSize, cornerRadius = corner)

                    if (cell.isRevealed) {
                        if (cell.isMine) {
                            val r = cellSize * 0.2f
                            for (angle in listOf(0f, 45f, 90f, 135f)) {
                                rotate(angle, pivot = center) {
                                    drawLine(mineColor, Offset(center.x - r * 1.5f, center.y), Offset(center.x + r * 1.5f, center.y), strokeWidth = cellSize * 0.07f)
                                }
                            }
                            drawCircle(mineColor, radius = r, center = center)
                        } else if (cell.surroundingMines > 0) {
                            val layout = numberLayouts[(cell.surroundingMines - 1).coerceIn(0, numberLayouts.lastIndex)]
                            drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
                            )
                        }
                    } else if (cell.isFlagged) {
                        translate(left = center.x - iconSize / 2, top = center.y - iconSize / 2) {
                            with(flagPainter) {
                                draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(flagColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- End of game effects ----------

/** Confetti on a win; a one-off blast over the board on a loss. */
@Composable
private fun ResultEffects(hasWon: Boolean) {
    if (hasWon) {
        MinesweeperExplosionConfetti()
        return
    }
    val explosionProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        explosionProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }
    if (explosionProgress.value >= 1f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = kotlin.math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
        val currentRadius = maxRadius * explosionProgress.value
        val alpha = (1f - explosionProgress.value).coerceIn(0f, 1f)
        val color = when {
            explosionProgress.value < 0.2f -> Color.White
            explosionProgress.value < 0.4f -> Color.Yellow
            explosionProgress.value < 0.7f -> Color(0xFFFF5722) // Orange
            else -> Color.Red
        }
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(color = color.copy(alpha = alpha), radius = currentRadius, center = center)
        if (explosionProgress.value > 0.1f) {
            drawCircle(color = Color.DarkGray.copy(alpha = alpha * 0.8f), radius = currentRadius * 0.8f, center = center)
        }
        if (explosionProgress.value > 0.2f) {
            drawCircle(color = Color.Black.copy(alpha = alpha * 0.6f), radius = currentRadius * 0.5f, center = center)
        }
    }
}

@Composable
fun MinesweeperExplosionConfetti() {
    val pieces = remember {
        List(180) {
            ExplosionPiece(
                angle = (210..330).random().toDouble(),
                speed = (20..50).random().toFloat(),
                rotationSpeed = (-15..15).random().toFloat(),
                color = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFFFF00FF),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFFFF3D00),
                    Color.White
                ).random(),
                size = (4..10).random().dp
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "explosion")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "elapsed"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height

        pieces.forEach { piece ->
            val t = elapsed * 35f
            val rad = Math.toRadians(piece.angle)
            val vx = Math.cos(rad).toFloat() * piece.speed
            val vy = Math.sin(rad).toFloat() * piece.speed
            val gravity = 0.45f

            val x = centerX + vx * t
            val y = centerY + vy * t + 0.5f * gravity * t * t

            if (y > -100 && y < centerY + 100) {
                val alpha = (1f - (elapsed - 0.7f) / 0.3f).coerceIn(0f, 1f)
                rotate(piece.rotationSpeed * t * 12f, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(piece.size.toPx(), piece.size.toPx())
                    )
                }
            }
        }
    }
}

data class ExplosionPiece(
    val angle: Double,
    val speed: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: androidx.compose.ui.unit.Dp
)

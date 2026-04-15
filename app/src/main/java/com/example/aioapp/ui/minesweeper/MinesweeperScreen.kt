package com.example.aioapp.ui.minesweeper

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon
import com.example.aioapp.ui.theme.LocalAppGradient
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import androidx.compose.ui.platform.LocalContext

@Composable
fun MinesweeperScreen(
    viewModel: MinesweeperViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val gameState by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    val appGradient = Brush.horizontalGradient(LocalAppGradient.current)
    
    var currentView by remember { mutableStateOf("Menu") } // Menu, DifficultySelect, CustomSelect, Game

    val difficulty by viewModel.difficulty.collectAsState()
    
    // Handle system back button
    BackHandler(enabled = currentView != "Menu") {
        when (currentView) {
            "Game" -> {
                viewModel.returnToMenu()
                currentView = "Menu"
            }
            "DifficultySelect" -> currentView = "Menu"
            "CustomSelect" -> currentView = "DifficultySelect"
        }
    }
    
    // Auto return to menu if difficulty is cleared (game properly ended when going back from Win/Loss)
    LaunchedEffect(difficulty, gameState) {
        if (difficulty == null && currentView == "Game") {
            currentView = "Menu"
        }
    }

    Scaffold(
        topBar = {
            AioTopBar(
                title = { Text(text = stringResource(R.string.feature_minesweeper), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    DefaultNavigationIcon(navController, drawerState, scope)
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentView) {
                "Menu" -> MainMenu(
                    onNewGame = { currentView = "DifficultySelect" },
                    onContinue = { 
                        currentView = "Game"
                        viewModel.resumeGame()
                    },
                    canContinue = difficulty != null,
                    appGradient = appGradient
                )
                "DifficultySelect" -> DifficultySelect(
                    onDifficultySelected = {
                        viewModel.startNewGame(it)
                        currentView = "Game"
                    },
                    onCustomSelected = { currentView = "CustomSelect" },
                    onBack = { currentView = "Menu" },
                    appGradient = appGradient
                )
                "CustomSelect" -> CustomDifficultySelect(
                    onStart = {
                        viewModel.startNewGame(it)
                        currentView = "Game"
                    },
                    onCancel = { currentView = "DifficultySelect" },
                    appGradient = appGradient
                )
                "Game" -> GameBoardView(
                    viewModel = viewModel,
                    appGradient = appGradient,
                    onReturnToMenu = { 
                        viewModel.returnToMenu()
                        currentView = "Menu"
                    }
                )
            }
        }
    }
}

@Composable
fun AppButton(text: String, onClick: () -> Unit, appGradient: Brush, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) appGradient else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun MainMenu(onNewGame: () -> Unit, onContinue: () -> Unit, canContinue: Boolean, appGradient: Brush) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppButton(stringResource(R.string.ms_new_game), onNewGame, appGradient)
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(stringResource(R.string.ms_continue), onContinue, appGradient, enabled = canContinue)
    }
}

@Composable
fun DifficultySelect(
    onDifficultySelected: (Difficulty) -> Unit,
    onCustomSelected: () -> Unit,
    onBack: () -> Unit,
    appGradient: Brush
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val difficulties = listOf(BeginnerDifficulty, EasyDifficulty, IntermediateDifficulty, AdvancedDifficulty)
        
        difficulties.forEach { diff ->
            AppButton(stringResource(diff.nameResId, diff.columns, diff.rows), { onDifficultySelected(diff) }, appGradient)
        }
        AppButton(stringResource(R.string.ms_custom), onCustomSelected, appGradient)
        
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.ms_back), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomDifficultySelect(onStart: (Difficulty) -> Unit, onCancel: () -> Unit, appGradient: Brush) {
    var width by remember { mutableFloatStateOf(10f) }
    var height by remember { mutableFloatStateOf(10f) }
    var mines by remember { mutableFloatStateOf(15f) }
    
    val maxMines = ((width * height) * 0.5f).toInt()
    if (mines > maxMines) mines = maxMines.toFloat()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.ms_custom), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(stringResource(R.string.ms_width, width.toInt()), fontWeight = FontWeight.Bold)
        Slider(value = width, onValueChange = { width = it }, valueRange = 5f..30f, steps = 25)
        
        Text(stringResource(R.string.ms_height, height.toInt()), fontWeight = FontWeight.Bold)
        Slider(value = height, onValueChange = { height = it }, valueRange = 5f..30f, steps = 25)
        
        Text(stringResource(R.string.ms_mines, mines.toInt()), fontWeight = FontWeight.Bold)
        Slider(value = mines, onValueChange = { mines = it }, valueRange = 1f..maxMines.toFloat().coerceAtLeast(1f), steps = maxMines - 1)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        AppButton(stringResource(R.string.ms_continue), { onStart(Difficulty(R.string.ms_custom, width.toInt(), height.toInt(), mines.toInt(), isCustom = true)) }, appGradient)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.ms_cancel), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameBoardView(viewModel: MinesweeperViewModel, appGradient: Brush, onReturnToMenu: () -> Unit) {
    val gameState by viewModel.gameState.collectAsState()
    val cells by viewModel.cells.collectAsState()
    val difficulty = viewModel.difficulty.collectAsState().value ?: return
    val isFlagMode by viewModel.isFlagMode.collectAsState()
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val minesRemaining by viewModel.minesRemaining.collectAsState()
    val vibrationsEnabled by viewModel.vibrationsEnabled.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = stringResource(R.string.ms_timer_desc))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format("%03d", timeElapsed), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (gameState == GameState.Playing) viewModel.pauseGame()
                        else if (gameState == GameState.Paused) viewModel.resumeGame()
                    }) {
                        Icon(
                            if (gameState == GameState.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume"
                        )
                    }
                    
                    IconButton(onClick = { viewModel.toggleFlagMode() }) {
                        Icon(
                            if (isFlagMode) Icons.Default.Flag else Icons.Default.AdsClick, 
                            contentDescription = "Toggle Mode"
                        )
                    }

                    IconButton(onClick = { viewModel.toggleVibrations() }) {
                        val iconColor = if (vibrationsEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Vibration,
                                contentDescription = "Toggle Vibrations",
                                tint = iconColor
                            )
                            if (!vibrationsEnabled) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = iconColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                    
                    IconButton(onClick = onReturnToMenu) {
                        Icon(Icons.Default.Home, contentDescription = "Menu")
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = stringResource(R.string.ms_mines_left_desc))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format("%03d", minesRemaining), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().weight(1f).clipToBounds()) {
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
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled=false){},
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.ms_pause), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                }
            } else if (gameState == GameState.Won || gameState == GameState.Lost) {
                WinLossOverlay(
                    hasWon = gameState == GameState.Won,
                    onReturn = onReturnToMenu,
                    onRestart = { viewModel.startNewGame(difficulty) },
                    appGradient = appGradient
                )
            }
        }
    }
}

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
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    
    val triggerVibration = {
        if (vibrationsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val cellSizeDp = 30.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val cellSize = with(density) { cellSizeDp.toPx() }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(difficulty) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    viewOffset += pan
                }
            }
            .pointerInput(difficulty, gameState, scale, viewOffset, vibrationsEnabled) {
                if (gameState != GameState.Playing) return@pointerInput
                detectTapGestures(
                    onTap = { pressPos ->
                        val boardWidth = difficulty.columns * cellSize
                        val boardHeight = difficulty.rows * cellSize
                        val centerOffsetX = (size.width - boardWidth) / 2f
                        val centerOffsetY = (size.height - boardHeight) / 2f
                        
                        val localX = (pressPos.x - size.width/2f) / scale + size.width/2f - viewOffset.x/scale - centerOffsetX
                        val localY = (pressPos.y - size.height/2f) / scale + size.height/2f - viewOffset.y/scale - centerOffsetY
                        
                        if (localX in 0f..boardWidth && localY in 0f..boardHeight) {
                            val col = (localX / cellSize).toInt()
                            val row = (localY / cellSize).toInt()
                            if (col in 0 until difficulty.columns && row in 0 until difficulty.rows) {
                                val index = row * difficulty.columns + col
                                if (isFlagMode && index in cells.indices && !cells[index].isRevealed) {
                                    triggerVibration()
                                }
                                onClick(col, row)
                            }
                        }
                    },
                    onLongPress = { pressPos ->
                        val boardWidth = difficulty.columns * cellSize
                        val boardHeight = difficulty.rows * cellSize
                        val centerOffsetX = (size.width - boardWidth) / 2f
                        val centerOffsetY = (size.height - boardHeight) / 2f
                        
                        val localX = (pressPos.x - size.width/2f) / scale + size.width/2f - viewOffset.x/scale - centerOffsetX
                        val localY = (pressPos.y - size.height/2f) / scale + size.height/2f - viewOffset.y/scale - centerOffsetY
                        
                        if (localX in 0f..boardWidth && localY in 0f..boardHeight) {
                            val col = (localX / cellSize).toInt()
                            val row = (localY / cellSize).toInt()
                            if (col in 0 until difficulty.columns && row in 0 until difficulty.rows) {
                                val index = row * difficulty.columns + col
                                if (index in cells.indices && !cells[index].isRevealed) {
                                    triggerVibration()
                                }
                                onLongClick(col, row)
                            }
                        }
                    }
                )
            }
    ) {
        val boardWidth = difficulty.columns * cellSize
        val boardHeight = difficulty.rows * cellSize
        val centerOffsetX = (constraints.maxWidth - boardWidth) / 2f
        val centerOffsetY = (constraints.maxHeight - boardHeight) / 2f

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
            for (cell in cells) {
                val cx = centerOffsetX + cell.x * cellSize
                val cy = centerOffsetY + cell.y * cellSize
                
                val backgroundColor = if (cell.isRevealed) {
                    if (cell.isMine) Color.Red else Color.LightGray
                } else {
                    Color.DarkGray
                }
                
                val pad = 2f
                drawRect(
                    color = backgroundColor,
                    topLeft = Offset(cx + pad, cy + pad),
                    size = Size(cellSize - pad * 2, cellSize - pad * 2),
                )
                
                if (cell.isRevealed) {
                    if (cell.isMine) {
                        drawCircle(
                            color = Color.Black,
                            radius = cellSize * 0.3f,
                            center = Offset(cx + cellSize / 2, cy + cellSize / 2)
                        )
                    } else if (cell.surroundingMines > 0) {
                        val textColor = when (cell.surroundingMines) {
                            1 -> Color.Blue
                            2 -> Color(0xFF007B00)
                            3 -> Color.Red
                            4 -> Color(0xFF00007B)
                            5 -> Color(0xFF7B0000)
                            6 -> Color(0xFF007B7B)
                            7 -> Color.Black
                            else -> Color.DarkGray
                        }
                        drawText(
                            textMeasurer = textMeasurer,
                            text = cell.surroundingMines.toString(),
                            topLeft = Offset(cx + cellSize * 0.3f, cy + cellSize * 0.15f),
                            style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        )
                    }
                } else if (cell.isFlagged) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(cx + cellSize * 0.35f, cy + cellSize * 0.2f),
                        size = Size(cellSize * 0.1f, cellSize * 0.6f)
                    )
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(cx + cellSize * 0.45f, cy + cellSize * 0.2f),
                        size = Size(cellSize * 0.3f, cellSize * 0.3f)
                    )
                }
            }
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

@Composable
fun WinLossOverlay(
    hasWon: Boolean, 
    onReturn: () -> Unit, 
    onRestart: () -> Unit, 
    appGradient: Brush
) {
    var showUi by remember { mutableStateOf(hasWon) }
    val explosionProgress = remember { Animatable(0f) }
    
    LaunchedEffect(hasWon) {
        if (!hasWon) {
            explosionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
            showUi = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        if (hasWon) {
            MinesweeperExplosionConfetti()
        } else if (explosionProgress.value < 1f) {
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
                
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = currentRadius,
                    center = Offset(size.width / 2, size.height / 2)
                )
                if (explosionProgress.value > 0.1f) {
                     drawCircle(
                        color = Color.DarkGray.copy(alpha = alpha * 0.8f),
                        radius = currentRadius * 0.8f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                if (explosionProgress.value > 0.2f) {
                     drawCircle(
                        color = Color.Black.copy(alpha = alpha * 0.6f),
                        radius = currentRadius * 0.5f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
            }
        }

        if (showUi) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val scale by animateFloatAsState(
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "scale"
                )
                
                Text(
                    text = if (hasWon) stringResource(R.string.ms_won) else stringResource(R.string.ms_lost),
                    color = if (hasWon) Color.Green else Color.Red,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(appGradient),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.ms_new_game), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReturn,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(appGradient),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(stringResource(R.string.ms_return_to_menu), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

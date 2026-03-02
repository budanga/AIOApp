package com.example.aioapp.ui.truco

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.core.model.TrucoGame
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.res.stringResource
import com.example.aioapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrucoScreen(
    viewModel: TrucoViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gradientColors = LocalAppGradient.current
    val appGradient = Brush.horizontalGradient(gradientColors)
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.feature_truco),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.truco_history_icon))
                    }
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.truco_reset_icon))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 15/30 Point Toggle
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    SegmentedButton(
                        selected = uiState.maxPoints == 15,
                        onClick = { viewModel.setMaxPoints(15) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.truco_15_points), fontFamily = RobotoMono)
                    }
                    SegmentedButton(
                        selected = uiState.maxPoints == 30,
                        onClick = { viewModel.setMaxPoints(30) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.truco_30_points), fontFamily = RobotoMono)
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // NOS Column
                    TeamColumn(
                        name = "Nos",
                        points = uiState.nosPoints,
                        maxPoints = uiState.maxPoints,
                        onIncrement = { viewModel.incrementNos() },
                        onDecrement = { viewModel.decrementNos() },
                        appGradient = appGradient,
                        modifier = Modifier.weight(1f)
                    )

                    // ELLOS Column
                    TeamColumn(
                        name = "Ellos",
                        points = uiState.ellosPoints,
                        maxPoints = uiState.maxPoints,
                        onIncrement = { viewModel.incrementEllos() },
                        onDecrement = { viewModel.decrementEllos() },
                        appGradient = appGradient,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Win Overlay
            AnimatedVisibility(
                visible = uiState.isGameOver,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize()
            ) {
                WinOverlay(
                    winner = uiState.winner ?: "",
                    onReset = { viewModel.reset() },
                    appGradient = appGradient
                )
            }

            // History Dialog (Overlay)
            if (showHistory) {
                HistoryDialog(
                    history = uiState.history,
                    onDismiss = { showHistory = false },
                    onClear = { viewModel.clearHistory() },
                    appGradient = appGradient
                )
            }
        }
    }
}

@Composable
fun HistoryDialog(
    history: List<TrucoGame>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    appGradient: Brush
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.truco_history_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = RobotoMono),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (history.isEmpty()) {
                            Text(
                                stringResource(R.string.truco_history_empty),
                                fontFamily = RobotoMono,
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(history.size) { index ->
                                        val game = history[index]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.truco_won_label, game.winner.uppercase()),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontFamily = RobotoMono,
                                                        color = if (game.winner == "Nos") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(game.timestamp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = RobotoMono,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("NOS", style = MaterialTheme.typography.labelSmall, fontFamily = RobotoMono)
                                                        Text(
                                                            game.nosPoints.toString(),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = RobotoMono
                                                        )
                                                    }
                                                    Text(
                                                        "VS",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        fontFamily = RobotoMono
                                                    )
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("ELLOS", style = MaterialTheme.typography.labelSmall, fontFamily = RobotoMono)
                                                        Text(
                                                            game.ellosPoints.toString(),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = RobotoMono
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = stringResource(R.string.truco_mode_label, game.maxPoints),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = RobotoMono,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                        ) {
                            if (history.isNotEmpty()) {
                                TextButton(onClick = onClear) {
                                    Text(
                                        stringResource(R.string.truco_history_clear),
                                        color = MaterialTheme.colorScheme.error,
                                        fontFamily = RobotoMono,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .width(100.dp)
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(appGradient)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = LocalIndication.current
                                    ) { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.truco_history_close),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMono
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WinOverlay(
    winner: String,
    onReset: () -> Unit,
    appGradient: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        ConfettiEffect()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "winScale")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Text(
                text = stringResource(R.string.truco_winner_label),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.7f)
            )

            Text(
                text = winner.uppercase(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                fontFamily = RobotoMono,
                color = Color.White,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(appGradient),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.truco_play_again),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = RobotoMono
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    val pieces = remember {
        List(100) {
            ConfettiPiece(
                x = (0..1000).random().toFloat() / 1000f,
                y = (0..1000).random().toFloat() / 1000f,
                color = listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Green, Color.Red).random(),
                speed = (5..15).random().toFloat() / 1000f,
                rotation = (0..360).random().toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "elapsed"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val yPos = ((piece.y + elapsed * piece.speed * 10) % 1.0f) * size.height
            val xPos = piece.x * size.width

            rotate(piece.rotation + elapsed * 360) {
                drawRect(
                    color = piece.color,
                    topLeft = Offset(xPos, yPos),
                    size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
    }
}

data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val rotation: Float
)

@Composable
fun TeamColumn(
    name: String,
    points: Int,
    maxPoints: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    appGradient: Brush,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = RobotoMono,
            fontWeight = FontWeight.Bold,
            color = if (name == "Nos") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val teamColor = if (name == "Nos") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            TrucoPointsDisplay(points = points, maxPoints = maxPoints, color = teamColor)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = points.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.Black
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.truco_decrement))
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(appGradient)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.truco_increment), tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun TrucoPointsDisplay(points: Int, maxPoints: Int, color: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rows = 3
        val cols = if (maxPoints == 15) 1 else 2

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0 until cols) {
                    val index = if (maxPoints == 15) r else (r * 2 + c)
                    val boxPoints = (points - (index * 5)).coerceIn(0, 5)
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TrucoBox(points = boxPoints, strokeColor = color)
                    }
                }
            }
        }
    }
}

@Composable
fun TrucoBox(points: Int, strokeColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.5.dp.toPx()
            val padding = 6.dp.toPx()

            val topLeft = Offset(padding, padding)
            val topRight = Offset(size.width - padding, padding)
            val bottomLeft = Offset(padding, size.height - padding)
            val bottomRight = Offset(size.width - padding, size.height - padding)

            // 1st point: Top
            if (points >= 1) {
                // Glow effect
                drawLine(strokeColor.copy(alpha = 0.3f), topLeft, topRight, strokeWidth * 2.5f, StrokeCap.Round)
                drawLine(strokeColor, topLeft, topRight, strokeWidth, StrokeCap.Round)
            }
            // 2nd point: Right
            if (points >= 2) {
                drawLine(strokeColor.copy(alpha = 0.3f), topRight, bottomRight, strokeWidth * 2.5f, StrokeCap.Round)
                drawLine(strokeColor, topRight, bottomRight, strokeWidth, StrokeCap.Round)
            }
            // 3rd point: Bottom
            if (points >= 3) {
                drawLine(strokeColor.copy(alpha = 0.3f), bottomRight, bottomLeft, strokeWidth * 2.5f, StrokeCap.Round)
                drawLine(strokeColor, bottomRight, bottomLeft, strokeWidth, StrokeCap.Round)
            }
            // 4th point: Left
            if (points >= 4) {
                drawLine(strokeColor.copy(alpha = 0.3f), bottomLeft, topLeft, strokeWidth * 2.5f, StrokeCap.Round)
                drawLine(strokeColor, bottomLeft, topLeft, strokeWidth, StrokeCap.Round)
            }
            // 5th point: Diagonal
            if (points >= 5) {
                drawLine(strokeColor.copy(alpha = 0.3f), topLeft, bottomRight, strokeWidth * 2.5f, StrokeCap.Round)
                drawLine(strokeColor, topLeft, bottomRight, strokeWidth, StrokeCap.Round)
            }
        }
    }
}

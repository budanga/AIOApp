package com.example.aioapp.ui.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.core.model.PomodoroMode
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono

@Composable
private fun PomodoroMode.label(): String = when (this) {
    PomodoroMode.WORK        -> stringResource(R.string.pomodoro_mode_work)
    PomodoroMode.SHORT_BREAK -> stringResource(R.string.pomodoro_mode_short_break)
    PomodoroMode.LONG_BREAK  -> stringResource(R.string.pomodoro_mode_long_break)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    // Kept as State and read only where needed: the timer ticks every second and must not recompose the whole screen
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    // Derived so that reading them only invalidates when they actually change, not on every tick
    val currentMode by remember { derivedStateOf { uiState.value.currentMode } }
    val isRunning by remember { derivedStateOf { uiState.value.isRunning } }
    val gradientColors = LocalAppGradient.current
    val accentBrush = SolidColor(MaterialTheme.colorScheme.primary)
    val context = LocalContext.current

    // Asked when the user starts a timer, the first moment the app needs to post notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.start() }
    val onStart = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.start()
    }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.feature_pomodoro),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ModeSelector(
                currentMode = currentMode,
                onSelect = viewModel::setMode
            )

            Spacer(modifier = Modifier.weight(0.3f))

            TimerDial(
                progress = { uiState.value.progress },
                timeLeft = { uiState.value.timeLeft },
                mode = currentMode,
                gradientColors = gradientColors
            )

            Spacer(modifier = Modifier.weight(0.7f))

            Controls(
                isRunning = isRunning,
                accentBrush = accentBrush,
                onStart = onStart,
                onPause = viewModel::pause,
                onReset = viewModel::reset
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(currentMode: PomodoroMode, onSelect: (PomodoroMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        PomodoroMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PomodoroMode.entries.size)
            ) {
                Text(
                    text = mode.label(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = RobotoMono
                )
            }
        }
    }
}

@Composable
private fun TimerDial(
    progress: () -> Float,
    timeLeft: () -> Long,
    mode: PomodoroMode,
    gradientColors: List<Color>
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val sweepBrush = remember(gradientColors) {
        Brush.sweepGradient(if (gradientColors.size >= 2) gradientColors else listOf(Color.Cyan, Color.Magenta))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp)
    ) {
        ProgressRing(progress, surfaceVariant, sweepBrush)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TimeText(timeLeft)
            Text(
                text = mode.label(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = RobotoMono
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: () -> Float, trackColor: Color, brush: Brush) {
    // Glides between the one-second steps instead of jumping
    val animatedProgress = animateFloatAsState(
        targetValue = progress(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 12.dp.toPx()
        drawCircle(
            color = trackColor,
            radius = size.minDimension / 2,
            style = Stroke(width = stroke)
        )
        // Read in the draw phase: animation frames only redraw, they don't recompose
        drawArc(
            brush = brush,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress.value,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/** Recomposes on every tick, together with [ProgressRing]; nothing else does. */
@Composable
private fun TimeText(timeLeft: () -> Long) {
    Text(
        text = formatTime(timeLeft()),
        style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = RobotoMono
        )
    )
}

@Composable
private fun Controls(
    isRunning: Boolean,
    accentBrush: Brush,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Surface(
            modifier = Modifier
                .width(140.dp)
                .height(56.dp)
                .clip(MaterialTheme.shapes.large),
            color = Color.Transparent,
            onClick = { if (isRunning) onPause() else onStart() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) stringResource(R.string.pomodoro_pause) else stringResource(R.string.pomodoro_start),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = RobotoMono
                )
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.width(140.dp).height(56.dp),
            shape = MaterialTheme.shapes.large,
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = accentBrush)
        ) {
            Text(
                stringResource(R.string.pomodoro_reset),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = RobotoMono
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}

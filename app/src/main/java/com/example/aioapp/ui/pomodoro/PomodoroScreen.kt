package com.example.aioapp.ui.pomodoro

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gradientColors = LocalAppGradient.current

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
            // Mode Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                PomodoroMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.currentMode == mode,
                        onClick = { viewModel.setMode(mode) },
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

            Spacer(modifier = Modifier.weight(0.3f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track
                    drawCircle(
                        color = surfaceVariant,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // Gradient Progress
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = if (gradientColors.size >= 2) gradientColors else listOf(Color.Cyan, Color.Magenta)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * uiState.progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(uiState.timeLeft),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RobotoMono
                        )
                    )
                    Text(
                        text = uiState.currentMode.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = RobotoMono
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.7f))

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
                    onClick = {
                        if (uiState.isRunning) viewModel.pause() else viewModel.start()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.isRunning) stringResource(R.string.pomodoro_pause) else stringResource(R.string.pomodoro_start),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontFamily = RobotoMono
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.width(140.dp).height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
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
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}

package com.example.aioapp.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aioapp.R

data class Functionality(
    val nameResId: Int,
    val icon: ImageVector,
    val route: String
)

val functionalities = listOf(
    Functionality(R.string.feature_file_manager, Icons.Default.Folder, "filemanager"),
    Functionality(R.string.feature_notes, Icons.AutoMirrored.Filled.Note, "notes"),
    Functionality(R.string.feature_pomodoro, Icons.Default.Timer, "pomodoro"),
    Functionality(R.string.feature_unit_converter, Icons.Default.SwapHoriz, "unitconverter"),
    Functionality(R.string.feature_truco, Icons.Default.EmojiEvents, "truco")
)

package com.example.aioapp.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

data class Functionality(
    val name: String,
    val icon: ImageVector,
    val route: String
)

val functionalities = listOf(
    Functionality(
        name = "File Manager",
        icon = Icons.Default.Folder,
        route = "filemanager"
    ),
    Functionality(
        name = "Notes",
        icon = Icons.AutoMirrored.Filled.Note,
        route = "notes"
    ),
    Functionality(
        name = "Pomodoro",
        icon = Icons.Default.Timer,
        route = "pomodoro"
    ),
    Functionality(
        name = "Unit Converter",
        icon = Icons.Default.Calculate,
        route = "unitconverter"
    )
)

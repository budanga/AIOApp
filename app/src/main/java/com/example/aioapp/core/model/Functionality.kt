package com.example.aioapp.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Folder
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
    )
)


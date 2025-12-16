package com.example.aioapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.theme.GradientEnd
import com.example.aioapp.ui.theme.GradientMid1
import com.example.aioapp.ui.theme.GradientMid2
import com.example.aioapp.ui.theme.GradientMid3
import com.example.aioapp.ui.theme.GradientStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(currentScreen: String, navController: NavController, drawerState: DrawerState, scope: CoroutineScope) {
    val gradientColors = Brush.horizontalGradient(
        colors = listOf(
            GradientStart,
            GradientMid1,
            GradientMid2,
            GradientMid3,
            GradientEnd
        )
    )
    val fontFamily = FontFamily(Font(R.font.bbhbogle_regular))

    CenterAlignedTopAppBar(
        title = {
            if (currentScreen == "home") {
                Text(
                    text = "AIOApp",
                    style = TextStyle(
                        brush = gradientColors,
                        fontFamily = fontFamily,
                        fontSize = 32.sp
                    )
                )
            } else {
                val title = if (currentScreen == "filemanager") {
                    "File Manager"
                } else {
                    currentScreen.replaceFirstChar { it.uppercase() }
                }
                Text(text = title)
            }
        },
        navigationIcon = {
            if (navController.previousBackStackEntry != null) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
        }
    )
}

package com.example.aioapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.theme.LocalAppGradient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AioTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets(0)
) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        modifier = modifier,
        windowInsets = windowInsets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    currentScreen: String,
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    val gradientColors = LocalAppGradient.current

    AioTopBar(
        title = {
            if (currentScreen == "home") {
                Text(
                    text = "AIOApp",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = gradientColors
                        ),
                        fontFamily = FontFamily(Font(R.font.bbhbogle_regular)),
                        fontSize = 40.sp
                    )
                )
            } else {
                val titleString = when (currentScreen) {
                    "filemanager" -> "File Manager"
                    else -> currentScreen.replaceFirstChar { it.uppercase() }
                }
                Text(text = titleString)
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
        },
        actions = actions,
        colors = colors
    )
}

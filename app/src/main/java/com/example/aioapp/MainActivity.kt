package com.example.aioapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aioapp.core.navigation.AppNavHost
import com.example.aioapp.ui.components.AppDrawer
import com.example.aioapp.ui.components.TopAppBar
import com.example.aioapp.ui.theme.AIOAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var navigationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switches to postSplashScreenTheme (Theme.AIOApp) and keeps the splash until the stored theme is known
        installSplashScreen().setKeepOnScreenCondition { viewModel.theme.value == null }
        super.onCreate(savedInstanceState)

        navigationIntent = intent

        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (theme) {
                "Dark" -> true
                "Light" -> false
                else -> systemDarkTheme
            }

            // Status/navigation bar icons follow the app's theme, not the system's: dark icons on the light theme
            LaunchedEffect(useDarkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { useDarkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            AIOAppTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentScreen = backStackEntry?.destination?.route ?: "home"
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                var gesturesEnabled by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current

                // Handle navigation from notification intent
                LaunchedEffect(navigationIntent) {
                    navigationIntent?.let { intent ->
                        if (intent.action == ACTION_NAVIGATE_POMODORO) {
                            navController.navigate("pomodoro") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            // Reset state so it doesn't re-navigate on rotation
                            navigationIntent = null
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = gesturesEnabled || drawerState.currentValue != DrawerValue.Closed,
                    drawerContent = { AppDrawer(navController = navController, drawerState = drawerState, scope = scope) },
                    modifier = Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            gesturesEnabled = down.position.x < 100.dp.toPx()

                            // Wait for the gesture to complete before resetting gesturesEnabled to true.
                            // This ensures the NEXT gesture starts with gesturesEnabled = true.
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.all { !it.pressed }) break
                            }
                            gesturesEnabled = true
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            if (currentScreen !in screensWithOwnTopBar) {
                                TopAppBar(currentScreen = currentScreen, navController = navController, drawerState = drawerState, scope = scope)
                            }
                        }
                    ) { padding ->
                        AppNavHost(navController = navController, padding = padding, drawerState = drawerState)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationIntent = intent
    }

    companion object {
        const val ACTION_NAVIGATE_POMODORO = "com.example.aioapp.ACTION_NAVIGATE_POMODORO"

        // Screens that draw their own top bar; the shared one would be drawn on top of it.
        private val screensWithOwnTopBar = setOf(
            "home", "filemanager", "notes", "pomodoro", "truco", "unitconverter", "minesweeper", "bbq"
        )
    }
}
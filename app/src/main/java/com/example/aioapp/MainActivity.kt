package com.example.aioapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aioapp.core.navigation.AppNavHost
import com.example.aioapp.ui.components.AppDrawer
import com.example.aioapp.ui.components.TopAppBar
import com.example.aioapp.ui.theme.AIOAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var navigationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AIOApp)
        
        navigationIntent = intent

        setContent {
            val theme by viewModel.theme.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (theme) {
                "Dark" -> true
                "Light" -> false
                "System" -> systemDarkTheme
                else -> systemDarkTheme
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AIOAppTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentScreen = backStackEntry?.destination?.route ?: "home"
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

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
                    drawerContent = { AppDrawer(navController = navController, drawerState = drawerState, scope = scope) }
                ) {
                    Scaffold(
                        topBar = {
                            if (currentScreen != "filemanager" && currentScreen != "notes") {
                                TopAppBar(currentScreen = currentScreen, navController = navController, drawerState = drawerState, scope = scope)
                            }
                        }
                    ) { padding ->
                        AppNavHost(navController = navController, viewModel = viewModel, padding = padding, drawerState = drawerState)
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
    }
}
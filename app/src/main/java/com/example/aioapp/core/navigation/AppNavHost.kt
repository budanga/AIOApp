package com.example.aioapp.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aioapp.MainViewModel
import com.example.aioapp.ui.home.HomeScreen
import com.example.aioapp.ui.settings.SettingsScreen
import com.example.aioapp.ui.filemanager.FileManagerScreen

@Composable
fun AppNavHost(navController: NavHostController, viewModel: MainViewModel, padding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(padding = padding)
        }

        composable("filemanager") {
            FileManagerScreen()
        }

        composable("settings") {
            SettingsScreen(viewModel = viewModel, padding = padding)
        }
    }
}

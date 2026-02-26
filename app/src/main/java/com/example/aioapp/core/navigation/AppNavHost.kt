package com.example.aioapp.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aioapp.MainViewModel
import com.example.aioapp.ui.filemanager.FileManagerScreen
import com.example.aioapp.ui.home.HomeScreen
import com.example.aioapp.ui.notes.NotesScreen
import com.example.aioapp.ui.notes.NotesViewModel
import com.example.aioapp.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    padding: PaddingValues,
    drawerState: DrawerState
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300)) }
    ) {
        composable("home") {
            HomeScreen(padding = padding, navController = navController)
        }

        composable("filemanager") {
            FileManagerScreen(navController = navController)
        }

        composable("notes") {
            val notesViewModel: NotesViewModel = viewModel()
            NotesScreen(
                viewModel = notesViewModel,
                padding = padding,
                navController = navController,
                drawerState = drawerState
            )
        }

        composable("settings") {
            SettingsScreen(viewModel = viewModel, padding = padding)
        }
    }
}

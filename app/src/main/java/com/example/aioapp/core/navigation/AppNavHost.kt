package com.example.aioapp.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.aioapp.ui.filemanager.FileManagerScreen
import com.example.aioapp.ui.home.HomeScreen
import com.example.aioapp.ui.notes.NotesScreen
import com.example.aioapp.ui.notes.NotesViewModel
import com.example.aioapp.ui.pomodoro.PomodoroScreen
import com.example.aioapp.ui.pomodoro.PomodoroViewModel
import com.example.aioapp.ui.settings.SettingsScreen
import com.example.aioapp.ui.settings.SettingsViewModel
import com.example.aioapp.ui.unitconverter.UnitConverterScreen
import com.example.aioapp.ui.unitconverter.UnitConverterViewModel
import com.example.aioapp.ui.paymentcomparator.PaymentComparatorScreen
import com.example.aioapp.ui.paymentcomparator.PaymentComparatorViewModel
import com.example.aioapp.ui.truco.TrucoScreen
import com.example.aioapp.ui.truco.TrucoViewModel
import com.example.aioapp.ui.minesweeper.MinesweeperScreen
import com.example.aioapp.ui.minesweeper.MinesweeperViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
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
            HomeScreen(navController = navController, drawerState = drawerState)
        }

        composable("filemanager") {
            FileManagerScreen(navController = navController)
        }

        composable("notes") {
            val notesViewModel: NotesViewModel = hiltViewModel()
            NotesScreen(
                viewModel = notesViewModel,
                padding = padding,
                navController = navController,
                drawerState = drawerState
            )
        }

        composable("pomodoro") {
            val pomodoroViewModel: PomodoroViewModel = hiltViewModel()
            PomodoroScreen(
                viewModel = pomodoroViewModel,
                padding = padding,
                navController = navController
            )
        }

        composable("unitconverter") {
            val unitConverterViewModel: UnitConverterViewModel = hiltViewModel()
            UnitConverterScreen(
                viewModel = unitConverterViewModel,
                padding = padding,
                navController = navController
            )
        }

        composable("truco") {
            val trucoViewModel: TrucoViewModel = hiltViewModel()
            TrucoScreen(
                viewModel = trucoViewModel,
                padding = padding,
                navController = navController
            )
        }

        composable("paymentcomparator") {
            val paymentComparatorViewModel: PaymentComparatorViewModel = hiltViewModel()
            PaymentComparatorScreen(
                viewModel = paymentComparatorViewModel,
                padding = padding,
                navController = navController
            )
        }

        composable("minesweeper") {
            val minesweeperViewModel: MinesweeperViewModel = hiltViewModel()
            MinesweeperScreen(
                viewModel = minesweeperViewModel,
                navController = navController,
                drawerState = drawerState
            )
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = settingsViewModel, padding = padding)
        }
    }
}

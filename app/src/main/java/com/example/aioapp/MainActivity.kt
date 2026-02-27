package com.example.aioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import com.example.aioapp.core.navigation.AppNavHost
import com.example.aioapp.ui.components.AppDrawer
import com.example.aioapp.ui.theme.AIOAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AIOApp)
        setContent {
            val theme by viewModel.theme.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (theme) {
                "Dark" -> true
                "Light" -> false
                "System" -> systemDarkTheme
                else -> systemDarkTheme
            }
            AIOAppTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = { AppDrawer(navController = navController, drawerState = drawerState, scope = scope) }
                ) {
                    AppNavHost(navController = navController, viewModel = viewModel, padding = PaddingValues(0.dp), drawerState = drawerState)
                }
            }
        }
    }
}
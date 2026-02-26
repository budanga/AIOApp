package com.example.aioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.aioapp.core.navigation.AppNavHost
import com.example.aioapp.ui.theme.AIOAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIOAppTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                Scaffold { padding ->
                    AppNavHost(
                        navController = navController,
                        viewModel = mainViewModel,
                        padding = padding
                    )
                }
            }
        }
    }
}

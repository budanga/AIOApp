package com.example.aioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aioapp.core.navigation.AppNavHost
import com.example.aioapp.ui.theme.AIOAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIOAppTheme {
                AppNavHost()
            }
        }
    }
}

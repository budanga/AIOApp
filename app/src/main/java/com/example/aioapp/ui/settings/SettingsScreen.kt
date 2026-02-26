package com.example.aioapp.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.aioapp.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, padding: PaddingValues) {
    Text(text = "Settings Screen", modifier = Modifier.padding(padding))
}

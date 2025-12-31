package com.example.aioapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aioapp.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, padding: PaddingValues) {
    val currentTheme = viewModel.theme.collectAsState(initial = "System").value

    Column(modifier = Modifier.padding(padding)) {
        ThemeOption(label = "System", isSelected = currentTheme == "System") {
            viewModel.setTheme("System")
        }
        ThemeOption(label = "Dark mode", isSelected = currentTheme == "Dark") {
            viewModel.setTheme("Dark")
        }
        ThemeOption(label = "Light mode", isSelected = currentTheme == "Light") {
            viewModel.setTheme("Light")
        }
    }
}

@Composable
fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

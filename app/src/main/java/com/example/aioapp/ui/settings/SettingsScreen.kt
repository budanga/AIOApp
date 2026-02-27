package com.example.aioapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aioapp.MainViewModel

import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val currentTheme = viewModel.theme.collectAsState(initial = "System").value
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AioTopBar(
                title = { Text("Settings") },
                navigationIcon = { DefaultNavigationIcon(navController, drawerState, scope) }
            )
        }
    ) { padding ->

    Column(modifier = Modifier.padding(padding)) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Theme Category
        SettingsCategory(title = "Theme") {
            ThemeOption(label = "System", isSelected = currentTheme == "System") {
                viewModel.setTheme("System")
            }
            ThemeOption(label = "Light mode", isSelected = currentTheme == "Light") {
                viewModel.setTheme("Light")
            }
            ThemeOption(label = "Dark mode", isSelected = currentTheme == "Dark") {
                viewModel.setTheme("Dark")
            }
        }
    }
}
}

@Composable
fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column {
            content()
        }
    }
}

@Composable
fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

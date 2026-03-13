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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.aioapp.R
import com.example.aioapp.core.model.AppLanguage

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    padding: PaddingValues
) {
    val currentTheme by viewModel.theme.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()

    Column(modifier = Modifier.padding(padding)) {
        Spacer(modifier = Modifier.height(8.dp))

        // Theme Category
        SettingsCategory(title = stringResource(R.string.theme_title)) {
            ThemeOption(label = stringResource(R.string.theme_system), isSelected = currentTheme == "System") {
                viewModel.setTheme("System")
            }
            ThemeOption(label = stringResource(R.string.theme_light), isSelected = currentTheme == "Light") {
                viewModel.setTheme("Light")
            }
            ThemeOption(label = stringResource(R.string.theme_dark), isSelected = currentTheme == "Dark") {
                viewModel.setTheme("Dark")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Category
        SettingsCategory(title = stringResource(R.string.language_selection)) {
            LanguageOption(label = stringResource(R.string.language_system), isSelected = currentLanguage == AppLanguage.System) {
                viewModel.updateLanguage(AppLanguage.System)
            }
            LanguageOption(label = stringResource(R.string.language_english), isSelected = currentLanguage == AppLanguage.English) {
                viewModel.updateLanguage(AppLanguage.English)
            }
            LanguageOption(label = stringResource(R.string.language_spanish), isSelected = currentLanguage == AppLanguage.Spanish) {
                viewModel.updateLanguage(AppLanguage.Spanish)
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
        Column { content() }
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
        Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun LanguageOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

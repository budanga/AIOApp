package com.example.aioapp

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val THEME_KEY = stringPreferencesKey("theme")
private val Context.dataStore by preferencesDataStore("settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _theme = MutableStateFlow("System")
    val theme = _theme.asStateFlow()

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            _theme.value = themeName
        }
    }
}
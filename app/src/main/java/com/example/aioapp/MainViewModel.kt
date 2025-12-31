package com.example.aioapp

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val THEME_KEY = stringPreferencesKey("theme")
private val Context.dataStore by preferencesDataStore("settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val theme = getApplication<Application>().dataStore.data.map {
        it[THEME_KEY] ?: "System"
    }

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[THEME_KEY] = themeName
            }
        }
    }
}
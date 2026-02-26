package com.example.aioapp

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted

private val THEME_KEY = stringPreferencesKey("theme")
private val Context.dataStore by preferencesDataStore("settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val theme = getApplication<Application>().dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map {
            it[THEME_KEY] ?: "System"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "System"
        )

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit {
                it[THEME_KEY] = themeName
            }
        }
    }
}
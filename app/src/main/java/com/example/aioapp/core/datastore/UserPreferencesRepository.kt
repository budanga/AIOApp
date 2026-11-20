package com.example.aioapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.aioapp.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("theme")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val FAVORITES_KEY = stringPreferencesKey("favorite_features")

    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[THEME_KEY] ?: "System"
        }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppLanguage.fromTag(preferences[LANGUAGE_KEY] ?: "system")
        }

    /** Favorite feature routes, most recently favorited first. */
    val favoritesFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[FAVORITES_KEY]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        }
        .distinctUntilChanged()

    /** Adds the route to the front of the favorites, or removes it if it already was one. */
    suspend fun toggleFavorite(route: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = if (route in current) current - route else listOf(route) + current
            preferences[FAVORITES_KEY] = updated.joinToString(",")
        }
    }

    suspend fun updateTheme(themeName: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeName
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.tag
        }
    }
}

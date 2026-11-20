package com.example.aioapp


import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.datastore.UserPreferencesRepository
import com.example.aioapp.core.model.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /** null until the stored theme is read; the splash screen stays up meanwhile to avoid a theme flash. */
    val theme = userPreferencesRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val language = userPreferencesRepository.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.System
        )

    init {
        // Apply locale immediately on each emission so it is set on first launch,
        // after process death, and on change — without relying on recomposition.
        viewModelScope.launch {
            userPreferencesRepository.languageFlow.collect { language ->
                val localeTag = if (language == AppLanguage.System) "" else language.tag
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(localeTag)
                )
            }
        }
    }
}
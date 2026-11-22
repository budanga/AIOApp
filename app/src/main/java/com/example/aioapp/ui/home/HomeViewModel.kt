package com.example.aioapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.datastore.UserPreferencesRepository
import com.example.aioapp.core.model.Functionality
import com.example.aioapp.core.model.HomeSections
import com.example.aioapp.core.model.homeSections
import com.example.aioapp.core.model.orderFunctionalities
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeatureListState(
    val functionalities: List<Functionality>,
    val favoriteRoutes: Set<String>,
    val sections: HomeSections
)

/** Feature order, favorites and home sections, used by both the home screen and the navigation drawer. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /** null until the stored favorites are read, so the list doesn't jump into place after first draw. */
    val featureList: StateFlow<FeatureListState?> = userPreferencesRepository.favoritesFlow
        .map { favorites -> FeatureListState(orderFunctionalities(favorites), favorites.toSet(), homeSections(favorites)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleFavorite(route: String) {
        viewModelScope.launch { userPreferencesRepository.toggleFavorite(route) }
    }
}

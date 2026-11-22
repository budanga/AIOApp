package com.example.aioapp.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aioapp.R

/** Home sections, in the order they are shown. */
enum class FeatureCategory(val titleResId: Int) {
    Tools(R.string.home_section_tools),
    Leisure(R.string.home_section_leisure)
}

data class Functionality(
    val nameResId: Int,
    val icon: ImageVector,
    val route: String,
    val category: FeatureCategory
)

val functionalities = listOf(
    Functionality(R.string.feature_file_manager, Icons.Default.Folder, "filemanager", FeatureCategory.Tools),
    Functionality(R.string.feature_notes, Icons.AutoMirrored.Filled.Note, "notes", FeatureCategory.Tools),
    Functionality(R.string.feature_pomodoro, Icons.Default.Timer, "pomodoro", FeatureCategory.Tools),
    Functionality(R.string.feature_unit_converter, Icons.Default.SwapHoriz, "unitconverter", FeatureCategory.Tools),
    Functionality(R.string.feature_truco, Icons.Default.EmojiEvents, "truco", FeatureCategory.Leisure),
    Functionality(R.string.feature_payment_comparator, Icons.Default.CreditScore, "paymentcomparator", FeatureCategory.Tools),
    Functionality(R.string.feature_minesweeper, Icons.Default.GridOn, "minesweeper", FeatureCategory.Leisure),
    Functionality(R.string.feature_bbq, Icons.Default.Restaurant, "bbq", FeatureCategory.Tools)
)

data class HomeSections(
    val favorites: List<Functionality>,
    val byCategory: Map<FeatureCategory, List<Functionality>>
)

/**
 * Favorites row (most recently favorited first) plus every feature grouped by category in its
 * default order. Favorites stay in their category too, so the sections never shuffle when starring.
 */
fun homeSections(favoriteRoutes: List<String>): HomeSections {
    val byRoute = functionalities.associateBy { it.route }
    return HomeSections(
        favorites = favoriteRoutes.mapNotNull { byRoute[it] },
        byCategory = FeatureCategory.entries.associateWith { category ->
            functionalities.filter { it.category == category }
        }
    )
}

/**
 * Favorites first (in the given order, most recently favorited first), then the rest
 * in their default order. Shared by the home grid and the drawer so both always match.
 */
fun orderFunctionalities(favoriteRoutes: List<String>): List<Functionality> {
    val byRoute = functionalities.associateBy { it.route }
    val favorites = favoriteRoutes.mapNotNull { byRoute[it] }
    return favorites + functionalities.filter { it.route !in favoriteRoutes }
}

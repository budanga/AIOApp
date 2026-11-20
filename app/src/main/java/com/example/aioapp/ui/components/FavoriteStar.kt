package com.example.aioapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.aioapp.R

val FavoriteGold = Color(0xFFFFC107)
val FavoriteGray = Color(0xFF9E9E9E)

/** Gray star that turns gold when the feature is marked as favorite. */
@Composable
fun FavoriteStarButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(if (isFavorite) FavoriteGold else FavoriteGray, label = "favoriteTint")
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
            tint = tint
        )
    }
}

package com.example.aioapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aioapp.R
import com.example.aioapp.ui.theme.RobotoMono
import java.text.Normalizer

private val diacritics = "\\p{Mn}+".toRegex()

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

/** Case- and accent-insensitive match, so "conversor" finds "Conversor" and "pomodóro" finds "Pomodoro". */
fun matchesSearch(name: String, query: String): Boolean =
    query.isBlank() || name.normalizedForSearch().contains(query.trim().normalizedForSearch())

@Composable
fun FeatureSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    appGradient: Brush,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.border(1.5.dp, appGradient, RoundedCornerShape(8.dp)),
        placeholder = {
            // Narrow containers (the drawer) would otherwise wrap the hint and make the field taller
            Text(
                stringResource(R.string.search_hint),
                fontFamily = RobotoMono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.feature_search_clear))
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = TextStyle(fontFamily = RobotoMono)
    )
}

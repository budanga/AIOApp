package com.example.aioapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.example.aioapp.ui.theme.LocalAppGradient
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.core.model.functionalities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val gradientColors = LocalAppGradient.current
    val appGradient = remember(gradientColors) { Brush.horizontalGradient(gradientColors) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Start fresh every time the drawer is opened
    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            searchQuery = ""
            focusManager.clearFocus()
        }
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        FeatureSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            appGradient = appGradient,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        )
        val matching = functionalities.map { it to stringResource(it.nameResId) }
            .filter { (_, name) -> matchesSearch(name, searchQuery) }
        if (matching.isEmpty()) {
            Text(
                text = stringResource(R.string.feature_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
        }
        matching.forEach { (functionality, name) ->
            NavigationDrawerItem(
                label = { Text(name) },
                icon = { Icon(functionality.icon, contentDescription = name) },
                selected = false,
                onClick = {
                    navController.navigate(functionality.route)
                    scope.launch { drawerState.close() }
                }
            )
        }
        Spacer(Modifier.weight(1f))
        val settingsLabel = stringResource(R.string.nav_settings)
        NavigationDrawerItem(
            label = { Text(settingsLabel) },
            icon = { Icon(Icons.Default.Settings, contentDescription = settingsLabel) },
            selected = false,
            onClick = {
                navController.navigate("settings")
                scope.launch { drawerState.close() }
            }
        )
    }
}

package com.example.aioapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Spacer(Modifier.padding(16.dp))
        functionalities.forEach { functionality ->
            val name = stringResource(functionality.nameResId)
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

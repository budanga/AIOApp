package com.example.aioapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    navController: NavController,
    drawerState: androidx.compose.material3.DrawerState,
    scope: CoroutineScope
) {
    val features = listOf(
        "filemanager" to "File Manager"
    )

    ModalDrawerSheet {
        Spacer(Modifier.padding(16.dp))
        features.forEach { (route, name) ->
            NavigationDrawerItem(
                label = { Text(name) },
                selected = false,
                onClick = {
                    navController.navigate(route)
                    scope.launch { drawerState.close() }
                }
            )
        }
        Spacer(Modifier.weight(1f))
        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            selected = false,
            onClick = {
                navController.navigate("settings")
                scope.launch { drawerState.close() }
            }
        )
    }
}

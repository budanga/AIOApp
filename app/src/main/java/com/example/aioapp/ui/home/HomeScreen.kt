package com.example.aioapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Feature(val name: String, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFeatureClick: (String) -> Unit
) {
    val features = listOf(
        Feature("File Manager", "filemanager")
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AIOApp") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(features) { feature ->
                FeatureCard(feature = feature, onClick = { onFeatureClick(feature.route) })
            }
        }
    }
}

@Composable
fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = feature.name,
            modifier = Modifier.padding(16.dp)
        )
    }
}

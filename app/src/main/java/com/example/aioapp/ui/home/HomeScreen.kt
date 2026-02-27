package com.example.aioapp.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.core.model.Functionality
import com.example.aioapp.core.model.functionalities

import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon
import com.example.aioapp.ui.theme.LocalAppGradient
import androidx.compose.ui.unit.sp
import com.example.aioapp.R

@Composable
fun HomeScreen(navController: NavController, drawerState: DrawerState) {
    val gradientColors = LocalAppGradient.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AioTopBar(
                title = {
                    Text(
                        text = "AIOApp",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = gradientColors
                            ),
                            fontFamily = FontFamily(Font(R.font.bbhbogle_regular)),
                            fontSize = 40.sp
                        )
                    )
                },
                navigationIcon = {
                    DefaultNavigationIcon(navController, drawerState, scope)
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(functionalities) { index, functionality ->
                FunctionalityItem(functionality = functionality, index = index) {
                    navController.navigate(functionality.route)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionalityItem(
    functionality: Functionality,
    index: Int,
    onClick: () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, delayMillis = index * 40)
        )
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(150.dp)
            .padding(8.dp)
            .graphicsLayer {
                alpha = animatedProgress.value
                scaleX = 0.9f + (animatedProgress.value * 0.1f)
                scaleY = 0.9f + (animatedProgress.value * 0.1f)
                translationY = (1f - animatedProgress.value) * 40f
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = functionality.icon,
                contentDescription = functionality.name,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = functionality.name)
        }
    }
}

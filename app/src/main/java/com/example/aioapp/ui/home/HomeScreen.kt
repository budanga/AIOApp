package com.example.aioapp.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aioapp.ui.components.FavoriteStarButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.core.model.Functionality

import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon
import com.example.aioapp.ui.components.FeatureSearchField
import com.example.aioapp.ui.components.matchesSearch
import com.example.aioapp.ui.theme.BbhBogle
import com.example.aioapp.ui.theme.LocalAppGradient
import androidx.compose.ui.unit.sp
import com.example.aioapp.R

@Composable
fun HomeScreen(
    navController: NavController,
    drawerState: DrawerState,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val featureList by viewModel.featureList.collectAsStateWithLifecycle()
    val gradientColors = LocalAppGradient.current
    val appGradient = remember(gradientColors) { Brush.horizontalGradient(gradientColors) }
    val scope = rememberCoroutineScope()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    val orderedFunctionalities = featureList?.functionalities.orEmpty()
    val favoriteRoutes = featureList?.favoriteRoutes.orEmpty()
    val names = orderedFunctionalities.map { stringResource(it.nameResId) }
    val visibleFunctionalities = remember(searchQuery, names, orderedFunctionalities) {
        orderedFunctionalities.filterIndexed { i, _ -> matchesSearch(names[i], searchQuery) }
    }

    fun closeSearch() {
        isSearchExpanded = false
        searchQuery = ""
    }

    BackHandler(enabled = isSearchExpanded) { closeSearch() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            withFrameNanos { } // Wait until the field is composed and attached
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            Column {
                AioTopBar(
                    title = {
                        Text(
                            text = "AIOApp",
                            style = TextStyle(
                                brush = appGradient,
                                fontFamily = BbhBogle,
                                fontSize = 40.sp
                            )
                        )
                    },
                    navigationIcon = {
                        DefaultNavigationIcon(navController, drawerState, scope)
                    },
                    actions = {
                        IconButton(onClick = { if (isSearchExpanded) closeSearch() else isSearchExpanded = true }) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = stringResource(
                                    if (isSearchExpanded) R.string.feature_search_close else R.string.feature_search_open
                                )
                            )
                        }
                    }
                )
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    FeatureSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        appGradient = appGradient,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(searchFocusRequester)
                    )
                }
            }
        }
    ) { padding ->
        if (featureList == null) {
            // Favorites not loaded yet; draw nothing for this single frame instead of a list that reorders
        } else if (visibleFunctionalities.isEmpty()) {
            Text(
                text = stringResource(R.string.feature_search_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Keyed by route so cards that stay visible while filtering keep their state
                itemsIndexed(visibleFunctionalities, key = { _, functionality -> functionality.route }) { index, functionality ->
                    FunctionalityItem(
                        functionality = functionality,
                        index = index,
                        isFavorite = functionality.route in favoriteRoutes,
                        onToggleFavorite = { viewModel.toggleFavorite(functionality.route) },
                        modifier = Modifier.animateItem()
                    ) {
                        closeSearch()
                        navController.navigate(functionality.route)
                    }
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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val name = stringResource(functionality.nameResId)
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
        modifier = modifier
            .size(150.dp)
            .padding(8.dp)
            .graphicsLayer {
                alpha = animatedProgress.value
                scaleX = 0.9f + (animatedProgress.value * 0.1f)
                scaleY = 0.9f + (animatedProgress.value * 0.1f)
                translationY = (1f - animatedProgress.value) * 40f
            }
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = functionality.icon,
                contentDescription = name,
                modifier = Modifier.size(74.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        FavoriteStarButton(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
        )
      }
    }
}

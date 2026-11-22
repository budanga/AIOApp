package com.example.aioapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.core.model.FeatureCategory
import com.example.aioapp.core.model.Functionality
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.components.DefaultNavigationIcon
import com.example.aioapp.ui.components.FavoriteStarButton
import com.example.aioapp.ui.components.FeatureSearchField
import com.example.aioapp.ui.components.matchesSearch
import com.example.aioapp.ui.theme.BbhBogle
import com.example.aioapp.ui.theme.LocalAppGradient

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

    val allFunctionalities = featureList?.functionalities.orEmpty()
    val favoriteRoutes = featureList?.favoriteRoutes.orEmpty()
    val names = allFunctionalities.map { stringResource(it.nameResId) }
    val searchResults = remember(searchQuery, names, allFunctionalities) {
        allFunctionalities.filterIndexed { i, _ -> matchesSearch(names[i], searchQuery) }
    }

    fun closeSearch() {
        isSearchExpanded = false
        searchQuery = ""
    }

    fun open(functionality: Functionality) {
        closeSearch()
        navController.navigate(functionality.route)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(searchFocusRequester)
                    )
                }
            }
        }
    ) { padding ->
        val sections = featureList?.sections
        if (sections == null) {
            // Favorites not loaded yet; draw nothing for this single frame instead of a list that reorders
        } else if (searchQuery.isNotBlank()) {
            // While searching, a flat list: sections would only spread a couple of matches apart
            if (searchResults.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        FeatureGroup(
                            items = searchResults,
                            favoriteRoutes = favoriteRoutes,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpen = ::open
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (sections.favorites.isNotEmpty()) {
                    item(key = "favorites_title") { SectionTitle(stringResource(R.string.home_section_favorites)) }
                    item(key = "favorites") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sections.favorites, key = { it.route }) { functionality ->
                                FavoriteTile(
                                    functionality = functionality,
                                    onClick = { open(functionality) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
                FeatureCategory.entries.forEach { category ->
                    val items = sections.byCategory[category].orEmpty()
                    if (items.isNotEmpty()) {
                        item(key = "${category.name}_title") { SectionTitle(stringResource(category.titleResId)) }
                        item(key = category.name) {
                            FeatureGroup(
                                items = items,
                                favoriteRoutes = favoriteRoutes,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpen = ::open,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

/** Container and content colors for a feature's icon, so each category reads as a group. */
@Composable
private fun FeatureCategory.iconColors(): Pair<Color, Color> = when (this) {
    FeatureCategory.Tools -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    FeatureCategory.Leisure -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
private fun FavoriteTile(
    functionality: Functionality,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, content) = functionality.category.iconColors()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.width(128.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(functionality.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(functionality.nameResId),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeatureGroup(
    items: List<Functionality>,
    favoriteRoutes: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpen: (Functionality) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, functionality ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                FeatureRow(
                    functionality = functionality,
                    isFavorite = functionality.route in favoriteRoutes,
                    onToggleFavorite = { onToggleFavorite(functionality.route) },
                    onClick = { onOpen(functionality) }
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    functionality: Functionality,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val (container, content) = functionality.category.iconColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(functionality.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(functionality.nameResId),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        FavoriteStarButton(isFavorite = isFavorite, onToggle = onToggleFavorite)
    }
}

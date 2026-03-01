package com.example.aioapp.ui.unitconverter

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    viewModel: UnitConverterViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val gradientColors = LocalAppGradient.current
    val gradientBrush = Brush.linearGradient(colors = gradientColors)
    val textFieldShape = RoundedCornerShape(8.dp)
    val containerColor = MaterialTheme.colorScheme.background
    val snackbarHostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        viewModel.moveUnit(fromIndex, toIndex)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = {
                    Text(
                        text = "Unit Converter",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = containerColor,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Category Selector
            UnitCategoryTabs(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    viewModel.selectCategory(category)
                    focusManager.clearFocus()
                },
                gradientBrush = gradientBrush
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitSelector(
                    modifier = Modifier.weight(1f),
                    selectedUnit = uiState.fromUnit,
                    units = uiState.availableUnits,
                    gradientBrush = gradientBrush,
                    shape = textFieldShape,
                    onUnitSelected = { 
                        viewModel.onFromUnitChange(it)
                        focusManager.clearFocus()
                    }
                )

                OutlinedTextField(
                    value = uiState.inputValue,
                    onValueChange = { viewModel.onInputChange(it) },
                    placeholder = { 
                        Text(
                            text = "Value", 
                            color = Color.Gray,
                            fontFamily = RobotoMono
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, gradientBrush, textFieldShape),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = RobotoMono),
                    shape = textFieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results List with Drag and Drop and Animation
            AnimatedContent(
                targetState = uiState.selectedCategory,
                transitionSpec = {
                    (slideInHorizontally { width -> width / 4 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 4 } + fadeOut()
                    )
                },
                label = "categoryContentTransition"
            ) { targetCategory ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragContainer(dragDropState) {
                            viewModel.saveReorderedUnits()
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(uiState.results, key = { _, result -> result.unitName }) { index, result ->
                        DraggableItem(dragDropState, index) { isDragging ->
                            val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")
                            val scale by animateFloatAsState(if (isDragging) 1.02f else 1f, label = "scale")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .shadow(elevation.dp, textFieldShape),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = textFieldShape
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.unitName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = RobotoMono
                                        )
                                        if (targetCategory == UnitCategory.CURRENCY && result.rate != null) {
                                            Text(
                                                text = "Rate: 1 USD = ${"%.4f".format(result.rate)} ${result.symbol}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontFamily = RobotoMono,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = result.value,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            fontFamily = RobotoMono
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = result.symbol,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontFamily = RobotoMono
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (targetCategory == UnitCategory.CURRENCY && uiState.lastUpdated != null) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(uiState.lastUpdated!!))
                                Text(
                                    text = "Last updated: $date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontFamily = RobotoMono
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitCategoryTabs(
    selectedCategory: UnitCategory,
    onCategorySelected: (UnitCategory) -> Unit,
    gradientBrush: Brush
) {
    val categories = UnitCategory.entries
    val selectedIndex = categories.indexOf(selectedCategory)

    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                height = 3.dp,
                color = Color.Transparent // We'll use background to apply gradient
            )
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(gradientBrush)
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        categories.forEachIndexed { index, category ->
            val selected = selectedIndex == index
            Tab(
                selected = selected,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontFamily = RobotoMono,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(lazyListState, scope, onMove)
    }
    return state
}

class DragDropState(
    val lazyListState: LazyListState,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    private var draggingItemOffset by mutableStateOf(0f)

    internal val itemOffset: Float
        get() = draggingItemOffset

    private var initialItemOffset = 0
    
    private var dragJob: Job? = null

    fun onDragStart(offset: androidx.compose.ui.geometry.Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                draggedIndex = item.index
                initialItemOffset = item.offset
            }
    }

    fun onDragInterrupted(onDragEnd: () -> Unit) {
        draggedIndex = null
        draggingItemOffset = 0f
        dragJob?.cancel()
        onDragEnd()
    }

    fun onDrag(offset: androidx.compose.ui.geometry.Offset) {
        draggingItemOffset += offset.y
        
        val draggedItem = draggedIndex?.let { index ->
            lazyListState.layoutInfo.visibleItemsInfo.find { it.index == index }
        } ?: return

        val startOffset = draggedItem.offset + draggingItemOffset
        val endOffset = startOffset + draggedItem.size

        lazyListState.layoutInfo.visibleItemsInfo
            .filter { item -> item.index != draggedItem.index }
            .firstOrNull { item ->
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                
                if (draggingItemOffset > 0) {
                    // Dragging down
                    endOffset > itemEnd - (item.size / 2) && endOffset < itemEnd + (item.size / 2)
                } else {
                    // Dragging up
                    startOffset < itemStart + (item.size / 2) && startOffset > itemStart - (item.size / 2)
                }
            }?.let { targetItem ->
                val currentIndex = draggedItem.index
                val targetIndex = targetItem.index
                
                onMove(currentIndex, targetIndex)
                draggedIndex = targetIndex
                draggingItemOffset -= (targetItem.offset - draggedItem.offset)
            }
    }
}

fun Modifier.dragContainer(
    dragDropState: DragDropState,
    onDragEnd: () -> Unit
): Modifier = this.pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> dragDropState.onDragStart(offset) },
        onDragEnd = { dragDropState.onDragInterrupted(onDragEnd) },
        onDragCancel = { dragDropState.onDragInterrupted(onDragEnd) },
        onDrag = { change, dragAmount ->
            change.consume()
            dragDropState.onDrag(dragAmount)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.draggedIndex
    val draggingOffset = if (dragging) dragDropState.itemOffset else 0f
    
    Box(
        modifier = Modifier
            .animateItem(
                placementSpec = if (dragging) null else spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            )
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = draggingOffset
            }
    ) {
        content(dragging)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    modifier: Modifier = Modifier,
    selectedUnit: String,
    units: List<UnitInfo>,
    gradientBrush: Brush,
    shape: RoundedCornerShape,
    onUnitSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .border(1.5.dp, gradientBrush, shape),
            textStyle = LocalTextStyle.current.copy(fontFamily = RobotoMono),
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = unit.name, 
                            fontFamily = RobotoMono,
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    onClick = {
                        onUnitSelected(unit.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

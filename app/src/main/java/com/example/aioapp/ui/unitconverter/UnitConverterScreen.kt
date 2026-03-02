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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun resolveUnitName(unitId: String, defaultName: String): String {
    val resId = getUnitNameResId(unitId)
    return if (resId != null) stringResource(resId) else defaultName
}

@Composable
fun UnitCategory.label(): String {
    val resId = when (this) {
        UnitCategory.MASS -> R.string.unit_cat_mass
        UnitCategory.LENGTH -> R.string.unit_cat_length
        UnitCategory.TEMPERATURE -> R.string.unit_cat_temperature
        UnitCategory.SPEED -> R.string.unit_cat_speed
        UnitCategory.VOLUME -> R.string.unit_cat_volume
        UnitCategory.TIME -> R.string.unit_cat_time
        UnitCategory.STORAGE -> R.string.unit_cat_storage
        UnitCategory.ENERGY -> R.string.unit_cat_energy
        UnitCategory.PRESSURE -> R.string.unit_cat_pressure
        UnitCategory.ELECTRICAL -> R.string.unit_cat_electrical
        UnitCategory.CURRENCY -> R.string.unit_cat_currency
    }
    return stringResource(resId)
}

fun getUnitNameResId(unitId: String): Int? = when (unitId.lowercase()) {
    "kilogram" -> R.string.unit_kilogram
    "gram" -> R.string.unit_gram
    "milligram" -> R.string.unit_milligram
    "pound" -> R.string.unit_pound
    "ounce" -> R.string.unit_ounce
    "meter" -> R.string.unit_meter
    "kilometer" -> R.string.unit_kilometer
    "centimeter" -> R.string.unit_centimeter
    "millimeter" -> R.string.unit_millimeter
    "mile" -> R.string.unit_mile
    "yard" -> R.string.unit_yard
    "foot" -> R.string.unit_foot
    "inch" -> R.string.unit_inch
    "celsius" -> R.string.unit_celsius
    "fahrenheit" -> R.string.unit_fahrenheit
    "kelvin" -> R.string.unit_kelvin
    "meter_per_second" -> R.string.unit_meter_per_second
    "kilometer_per_hour" -> R.string.unit_kilometer_per_hour
    "mile_per_hour" -> R.string.unit_mile_per_hour
    "knot" -> R.string.unit_knot
    "foot_per_second" -> R.string.unit_foot_per_second
    "liter" -> R.string.unit_liter
    "milliliter" -> R.string.unit_milliliter
    "cubic_meter" -> R.string.unit_cubic_meter
    "gallon" -> R.string.unit_gallon
    "quart" -> R.string.unit_quart
    "pint" -> R.string.unit_pint
    "cup" -> R.string.unit_cup
    "second" -> R.string.unit_second
    "millisecond" -> R.string.unit_millisecond
    "minute" -> R.string.unit_minute
    "hour" -> R.string.unit_hour
    "day" -> R.string.unit_day
    "week" -> R.string.unit_week
    "byte" -> R.string.unit_byte
    "kilobyte" -> R.string.unit_kilobyte
    "megabyte" -> R.string.unit_megabyte
    "gigabyte" -> R.string.unit_gigabyte
    "terabyte" -> R.string.unit_terabyte
    "joule" -> R.string.unit_joule
    "kilojoule" -> R.string.unit_kilojoule
    "calorie" -> R.string.unit_calorie
    "kilocalorie" -> R.string.unit_kilocalorie
    "watt_hour" -> R.string.unit_watt_hour
    "kilowatt_hour" -> R.string.unit_kilowatt_hour
    "pascal" -> R.string.unit_pascal
    "kilopascal" -> R.string.unit_kilopascal
    "bar" -> R.string.unit_bar
    "psi" -> R.string.unit_psi
    "atmosphere" -> R.string.unit_atmosphere
    "ampere" -> R.string.unit_ampere
    "milliampere" -> R.string.unit_milliampere
    "microampere" -> R.string.unit_microampere
    "volt" -> R.string.unit_volt
    "ohm" -> R.string.unit_ohm
    "usd" -> R.string.unit_usd
    "eur" -> R.string.unit_eur
    "ars" -> R.string.unit_ars
    "clp" -> R.string.unit_clp
    "uyu" -> R.string.unit_uyu
    "brl" -> R.string.unit_brl
    "jpy" -> R.string.unit_jpy
    "cny" -> R.string.unit_cny
    else -> null
}

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
                        text = stringResource(R.string.feature_unit_converter),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
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
                    selectedUnitId = uiState.fromUnit,
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
                            text = stringResource(R.string.unit_converter_value_hint),
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
                                            text = resolveUnitName(result.unitId, result.unitName),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = RobotoMono
                                        )
                                        if (targetCategory == UnitCategory.CURRENCY && result.rate != null) {
                                            Text(
                                                text = stringResource(R.string.unit_converter_rate_label, "%.4f".format(result.rate), result.symbol),
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
                                    text = stringResource(R.string.unit_converter_last_updated, date),
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
                        text = category.label(),
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
    selectedUnitId: String,
    units: List<UnitInfo>,
    gradientBrush: Brush,
    shape: RoundedCornerShape,
    onUnitSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayFallback = units.find { it.id == selectedUnitId }?.name ?: selectedUnitId
    val displayName = resolveUnitName(selectedUnitId, displayFallback)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayName,
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
                            text = resolveUnitName(unit.id, unit.name),
                            fontFamily = RobotoMono,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onUnitSelected(unit.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

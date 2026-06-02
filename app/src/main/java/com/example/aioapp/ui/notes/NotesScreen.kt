package com.example.aioapp.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.NoteSortOrder
import com.example.aioapp.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        windowInsets = windowInsets,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors
    )
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    autoScrollToMatch: Boolean = false
) {
    val robotoMono = FontFamily(Font(R.font.roboto_mono_variable_font_wght))
    val highlightColor = LocalAppGradient.current.first().copy(alpha = 0.3f)

    val displayText = remember(text, query, autoScrollToMatch) {
        if (autoScrollToMatch && query.isNotBlank()) {
            val index = text.indexOf(query, ignoreCase = true)
            if (index > 50) {
                "..." + text.substring(index - 20)
            } else text
        } else text
    }

    val annotatedString = remember(displayText, query, highlightColor) {
        buildAnnotatedString {
            if (query.isBlank() || !displayText.contains(query, ignoreCase = true)) {
                append(displayText)
            } else {
                var currentIndex = 0
                val lowerText = displayText.lowercase()
                val lowerQuery = query.lowercase()

                while (currentIndex < displayText.length) {
                    val index = lowerText.indexOf(lowerQuery, currentIndex)
                    if (index == -1) {
                        append(displayText.substring(currentIndex))
                        break
                    }

                    append(displayText.substring(currentIndex, index))
                    withStyle(style = SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                        append(displayText.substring(index, index + query.length))
                    }
                    currentIndex = index + query.length
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = style.copy(fontFamily = robotoMono),
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    padding: PaddingValues,
    navController: NavController,
    drawerState: DrawerState
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(setOf<String>()) }
    var viewingNoteId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val searchFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val isSelectionMode by remember { derivedStateOf { selectedNoteIds.isNotEmpty() } }

    // Handle system back button
    BackHandler(enabled = isSelectionMode || viewingNoteId != null || showAddDialog || isSearchExpanded || searchQuery.isNotEmpty()) {
        if (isSelectionMode) {
            selectedNoteIds = emptySet()
        } else if (showAddDialog) {
            showAddDialog = false
        } else if (isSearchExpanded) {
            isSearchExpanded = false
            viewModel.onSearchQueryChange("")
        } else if (searchQuery.isNotEmpty()) {
            viewModel.onSearchQueryChange("")
        } else {
            viewingNoteId = null
        }
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    val homemadeApple = FontFamily(Font(R.font.homemade_apple))
    val robotoMono = FontFamily(Font(R.font.roboto_mono_variable_font_wght))
    val appGradientColors = LocalAppGradient.current
    val appGradient = Brush.horizontalGradient(colors = appGradientColors)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.imePadding()
            )
        },
        topBar = {
            if (viewingNoteId == null) {
                Column {
                    AppTopAppBar(
                        title = {
                            Text(
                                text = if (isSelectionMode) "${selectedNoteIds.size} Selected" else "Notes",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = robotoMono)
                            )
                        },
                        navigationIcon = {
                            if (isSelectionMode) {
                                IconButton(onClick = { selectedNoteIds = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            } else if (navController.previousBackStackEntry != null) {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        actions = {
                            if (isSelectionMode) {
                                IconButton(onClick = {
                                    viewModel.togglePinForSelected(selectedNoteIds)
                                    selectedNoteIds = emptySet()
                                }) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Toggle Pin", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    val count = selectedNoteIds.size
                                    viewModel.deleteNotes(selectedNoteIds)
                                    selectedNoteIds = emptySet()
                                    scope.launch {
                                        val msg = if (count == 1) "Note deleted successfully" else "Notes deleted successfully"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            } else if (!showAddDialog) {
                                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                                    Icon(
                                        imageVector = if (isSearchExpanded) Icons.Default.SearchOff else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        NoteSortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(if (order == NoteSortOrder.ALPHABETICAL) "Name" else order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                                onClick = {
                                                    viewModel.setSortOrder(order)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    AnimatedVisibility(
                        visible = isSearchExpanded && !showAddDialog,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .focusRequester(searchFocusRequester)
                                .border(1.5.dp, appGradient, RoundedCornerShape(8.dp)),
                            placeholder = { Text("Search notes...", fontFamily = robotoMono) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
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
                            textStyle = TextStyle(fontFamily = robotoMono)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode && viewingNoteId == null && !showAddDialog,
                enter = fadeIn(tween(200)) + scaleIn(animationSpec = tween(200)),
                exit = fadeOut(tween(200)) + scaleOut(animationSpec = tween(200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (notes.isEmpty() && searchQuery.isEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontFamily = homemadeApple, fontSize = 20.sp)) { append("Write ") }
                                withStyle(style = SpanStyle(fontSize = 20.sp)) { append("your first note!") }
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "fabScale"
                    )

                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (notes.isEmpty() && searchQuery.isEmpty()) 1.1f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer(scaleX = scale * pulseScale, scaleY = scale * pulseScale)
                            .shadow(if (isPressed) 4.dp else 12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(appGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Note",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = viewingNoteId,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.95f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "ScreenTransition"
            ) { targetViewingNoteId ->
                if (targetViewingNoteId == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (notes.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp + innerPadding.calculateTopPadding(),
                                    end = 16.dp,
                                    bottom = 8.dp + innerPadding.calculateBottomPadding()
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(notes, key = { it.id }) {
                                    NoteItem(
                                        note = it,
                                        isSelected = selectedNoteIds.contains(it.id),
                                        searchQuery = searchQuery,
                                        onLongClick = {
                                            if (!isSelectionMode) selectedNoteIds = setOf(it.id)
                                        },
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedNoteIds = if (selectedNoteIds.contains(it.id)) {
                                                    selectedNoteIds - it.id
                                                } else {
                                                    selectedNoteIds + it.id
                                                }
                                            } else {
                                                isSearchExpanded = false
                                                viewModel.onSearchQueryChange("")
                                                viewingNoteId = it.id
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isEmpty()) "No notes yet" else "No matches found",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = robotoMono
                                )
                            }
                        }
                    }
                } else {
                    val note = notes.find { it.id == targetViewingNoteId }
                    if (note != null) {
                        ViewEditNoteScreen(
                            note = note,
                            viewModel = viewModel,
                            onDismiss = { viewingNoteId = null },
                            appGradient = appGradient,
                            onUniqueError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Note title must be unique")
                                }
                            }
                        )
                    } else {
                        viewingNoteId = null
                    }
                }
            }
        }

        if (showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showAddDialog = false }
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                AddNoteDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, content, color ->
                        scope.launch {
                            if (viewModel.addNote(title, content, color)) {
                                showAddDialog = false
                            } else {
                                snackbarHostState.showSnackbar("Note title must be unique")
                            }
                        }
                    },
                    appGradient = appGradient
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateString = remember(note.createdAt) { dateFormat.format(Date(note.createdAt)) }
    val baseColor = Color(note.color)
    val isLightNote = baseColor.luminance() > 0.5f
    val contentColor = if (isLightNote) Color.Black else Color.White
    val appGradientColors = LocalAppGradient.current
    val selectionGradient = Brush.horizontalGradient(colors = appGradientColors)
    val robotoMono = FontFamily(Font(R.font.roboto_mono_variable_font_wght))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.5.dp, selectionGradient) else null,
        colors = CardDefaults.cardColors(containerColor = baseColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPinned) {
                        Surface(
                            color = contentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = contentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.notes_pinned),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = robotoMono,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }

            if (note.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HighlightedText(
                    text = note.title,
                    query = searchQuery,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                HighlightedText(
                    text = note.content,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    autoScrollToMatch = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewEditNoteScreen(
    note: Note,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit,
    appGradient: Brush,
    onUniqueError: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(note.createdAt))
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isDarkApp = LocalDarkTheme.current
    val baseColor = Color(note.color)
    val isLightNote = baseColor.luminance() > 0.5f
    val contentColor = if (isLightNote) Color.Black else Color.White

    val noteGradient = remember(baseColor, isDarkApp, isLightNote) {
        val shift = 0.35f
        val bottomColor = if (isDarkApp) {
            val target = if (isLightNote) Color(0xFF444444) else Color.Black
            lerp(baseColor, target, shift)
        } else {
            val target = if (!isLightNote) Color(0xFFBBBBBB) else Color.White
            lerp(baseColor, target, shift)
        }
        Brush.verticalGradient(
            colors = listOf(baseColor, bottomColor)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(noteGradient),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.imePadding()
            )
        },
        topBar = { },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabScale"
            )
            val rotation by animateFloatAsState(
                targetValue = if (isEditing) 360f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "iconRotation"
            )

            Box(
                modifier = Modifier
                    .imePadding()
                    .size(56.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .shadow(if (isPressed) 4.dp else 12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(appGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) {
                        if (isEditing) {
                            if (title.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Note title cannot be empty")
                                }
                                return@clickable
                            }
                            scope.launch {
                                if (viewModel.updateNote(note.id, title, content)) {
                                    isEditing = false
                                } else {
                                    snackbarHostState.showSnackbar("Note title must be unique")
                                }
                            }
                        } else {
                            isEditing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(100)) + scaleIn(animationSpec = tween(100)))
                            .togetherWith(fadeOut(animationSpec = tween(100)) + scaleOut(animationSpec = tween(100)))
                    },
                    label = "IconChange"
                ) { editing ->
                    Icon(
                        if (editing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (editing) "Save" else "Edit",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(rotationZ = rotation)
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp) // Apply horizontal padding to the whole scrollable column
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .offset(x = (-12).dp) // Align arrow perfectly with text left edge
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = isEditing,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)))
                        .togetherWith(fadeOut(animationSpec = tween(120)))
                },
                label = "EditTransition"
            ) { editing ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (editing) {
                        val bentoContainerColor = contentColor.copy(alpha = 0.05f)
                        val bentoBorderColor = contentColor.copy(alpha = 0.1f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, bentoBorderColor, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bentoContainerColor)
                        ) {
                            TextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = contentColor),
                                placeholder = { Text("Title", color = contentColor.copy(alpha = 0.5f)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = contentColor,
                                    unfocusedTextColor = contentColor
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, bentoBorderColor, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bentoContainerColor.copy(alpha = bentoContainerColor.alpha * 0.8f)))
                        {
                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                                placeholder = { Text("Start typing...", color = contentColor.copy(alpha = 0.5f)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = contentColor,
                                    unfocusedTextColor = contentColor
                                )
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.notes_created, dateString),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    appGradient: Brush
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val colors = listOf(Color.White, Color(0xFFFFF9C4), Color(0xFFFFCCBC), Color(0xFFC8E6C9), Color(0xFFBBDEFB), Color(0xFFF8BBD0))
    var selectedColor by remember { mutableStateOf(colors[0]) }
    val focusRequester = remember { FocusRequester() }

    val isFormComplete = title.isNotBlank()

    Surface(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "New Note",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, appGradient, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, appGradient, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Column {
                Text("Background Color:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 2.dp else 1.dp,
                                    color = if (selectedColor == color) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(44.dp).width(100.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .width(100.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFormComplete) appGradient else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)))
                        .clickable(
                            enabled = isFormComplete,
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        ) { onConfirm(title, content, selectedColor.toArgb()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add",
                        color = if (isFormComplete) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

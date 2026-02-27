package com.example.aioapp.ui.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AioTopBar
import com.example.aioapp.ui.theme.LocalAppGradient
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    navController: NavController,
    viewModel: FileManagerViewModel = viewModel(),
    theme: String = "System"
) {
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val canNavigateUp by viewModel.canNavigateUp.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardItem by viewModel.clipboardItem.collectAsState()
    val cutItemUris by viewModel.cutItemUris.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showFullPath by remember { mutableStateOf(false) }
    var infoItem by remember { mutableStateOf<Any?>(null) }
    var isSearchBarVisible by remember { mutableStateOf(false) }

    val appGradientColors = LocalAppGradient.current
    val appGradient = remember(appGradientColors) { Brush.horizontalGradient(colors = appGradientColors) }
    val electrolize = FontFamily(Font(R.font.electrolize_regular))

    val fullPath = remember(currentDirectory) {
        currentDirectory?.let { uri ->
            when (uri.scheme) {
                "file" -> uri.path ?: ""
                "content" -> {
                    val decoded = Uri.decode(uri.toString())
                    if (decoded.contains("primary:")) {
                        val path = decoded.substringAfterLast("primary:")
                        if (path.isEmpty()) "Internal Storage" else "Internal Storage/$path"
                    } else {
                        decoded
                    }
                }
                else -> uri.toString()
            }
        } ?: ""
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionsAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { _ ->
            viewModel.checkPermissionsAndLoad()
        }
    )

    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage.collectLatest {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = canNavigateUp || showFullPath || isSearchBarVisible || selectedItems.isNotEmpty() || showCreateFileDialog || showCreateFolderDialog) {
        if (showFullPath) {
            showFullPath = false
        } else if (isSearchBarVisible) {
            isSearchBarVisible = false
            viewModel.setSearchQuery("")
        } else if (selectedItems.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (showCreateFileDialog) {
            showCreateFileDialog = false
        } else if (showCreateFolderDialog) {
            showCreateFolderDialog = false
        } else {
            viewModel.navigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                currentDirectory?.let { directory ->
                    Column {
                        if (selectedItems.isNotEmpty()) {
                            SelectionTopAppBar(
                                selectedCount = selectedItems.size,
                                onClearSelection = viewModel::clearSelection,
                                onCopy = viewModel::copySelected,
                                onCut = viewModel::cutSelected,
                                onDelete = { showDeleteConfirmationDialog = true }
                            )
                        } else if (isSearchBarVisible) {
                            SearchTopAppBar(
                                query = searchQuery,
                                onQueryChange = viewModel::setSearchQuery,
                                onCloseSearch = {
                                    isSearchBarVisible = false
                                    viewModel.setSearchQuery("")
                                }
                            )
                        } else {
                            FileManagerTopAppBar(
                                currentDirectory = directory,
                                canNavigateUp = canNavigateUp,
                                onNavigateUp = {
                                    if (canNavigateUp) {
                                        viewModel.navigateUp()
                                    } else {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                            anim {
                                                enter = 0
                                                exit = 0
                                                popEnter = 0
                                                popExit = 0
                                            }
                                        }
                                    }
                                },
                                onSetSortOrder = viewModel::setSortOrder,
                                onShowCreateFolderDialog = { showCreateFolderDialog = true },
                                onShowSearch = { isSearchBarVisible = true },
                                isClipboardEmpty = clipboardItem == null,
                                onPaste = viewModel::paste,
                                onTitleClick = { showFullPath = true }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = currentDirectory != null && !isSearching && selectedItems.isEmpty(),
                    enter = fadeIn(tween(200)) + scaleIn(animationSpec = tween(200)),
                    exit = fadeOut(tween(200)) + scaleOut(animationSpec = tween(200))
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "fabScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .shadow(if (isPressed) 4.dp else 12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(appGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) { showCreateFileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Create file", 
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (currentDirectory != null) {
                    if (isSearching) {
                        SearchResultsList(
                            results = searchResults,
                            onFileClick = { viewModel.openFile(it.uri, it.mimeType) },
                            onDirectoryClick = { viewModel.navigateToDirectory(it.uri) },
                            onShowInfo = { infoItem = it }
                        )
                    } else {
                        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh() }) {
                            DirectoryContent(
                                viewModel = viewModel,
                                selectedItems = selectedItems,
                                cutItemUris = cutItemUris,
                                appGradient = appGradient,
                                onShowInfo = { infoItem = it }
                            )
                        }
                    }
                } else {
                    PermissionRequestScreen {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            launcher.launch(intent)
                        } else {
                            Toast.makeText(context, "All files access is required.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showFullPath,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showFullPath = false },
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(top = 104.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = fullPath,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (showCreateFileDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showCreateFileDialog = false }
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                CreateFileDialog(
                    onDismiss = { showCreateFileDialog = false },
                    onCreate = viewModel::createFile,
                    appGradient = appGradient,
                    titleFontFamily = electrolize
                )
            }
        }

        if (showCreateFolderDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showCreateFolderDialog = false }
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                CreateFolderDialog(
                    onDismiss = { showCreateFolderDialog = false },
                    onCreate = viewModel::createFolder,
                    appGradient = appGradient,
                    titleFontFamily = electrolize
                )
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteConfirmationDialog = false
            },
            onDismiss = { showDeleteConfirmationDialog = false }
        )
    }

    infoItem?.let { item ->
        InfoSheet(item = item, onDismiss = { infoItem = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(item: Any, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Item Details",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val (name, path, size, formattedDate, formattedCreation) = when (item) {
                is FileData -> listOf(item.name, item.path, item.formattedSize, item.formattedDate, FileManagerViewModel.formatDate(item.creationTime))
                is DirectoryData -> listOf(item.name, item.path, if (item.formattedSize.isNotEmpty()) item.formattedSize else "Calculating...", item.formattedDate, FileManagerViewModel.formatDate(item.creationTime))
                else -> listOf("", "", "", "", "")
            }

            InfoRow("Name", name)
            InfoRow("Path", path)
            InfoRow("Size", size)
            InfoRow("Modified", formattedDate)
            if (formattedCreation.isNotEmpty()) {
                InfoRow("Created", formattedCreation)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    AioTopBar(
        windowInsets = WindowInsets.statusBars,
        title = { Text("$selectedCount Selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        },
        actions = {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onCut) {
                Icon(Icons.Default.ContentCut, contentDescription = "Cut")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AioTopBar(
        windowInsets = WindowInsets.statusBars,
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Search files & folders...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    onFileClick: (FileData) -> Unit,
    onDirectoryClick: (DirectoryData) -> Unit,
    onShowInfo: (Any) -> Unit
) {
    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results found", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { result ->
                when (result) {
                    is SearchResult.Directory -> "dir_${result.data.uri}"
                    is SearchResult.File -> "file_${result.data.uri}"
                }
            }) { result ->
                when (result) {
                    is SearchResult.Directory -> SearchDirectoryItem(result.data, onClick = { onDirectoryClick(result.data) }, onInfo = { onShowInfo(result.data) })
                    is SearchResult.File -> SearchFileItem(result.data, onClick = { onFileClick(result.data) }, onInfo = { onShowInfo(result.data) })
                }
            }
        }
    }
}

@Composable
fun SearchDirectoryItem(directory: DirectoryData, onClick: () -> Unit, onInfo: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(directory.name) },
        supportingContent = { Text(directory.path, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = "Directory", tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Info") }
        }
    )
}

@Composable
fun SearchFileItem(file: FileData, onClick: () -> Unit, onInfo: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(file.name) },
        supportingContent = { Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "File") },
        trailingContent = {
            IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Info") }
        }
    )
}

@Composable
fun PermissionRequestScreen(onGrantAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permission Required", textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "To manage your files, please grant 'All files access' permission in settings. This allows the app to show and manage all folders and files on your device storage.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrantAccess) {
            Text("Grant Access")
        }
    }
}

@Composable
fun DirectoryContent(
    viewModel: FileManagerViewModel,
    selectedItems: Set<Uri>,
    cutItemUris: Set<Uri>,
    appGradient: Brush,
    onShowInfo: (Any) -> Unit
) {
    val directories by viewModel.sortedDirectories.collectAsState()
    val files by viewModel.sortedFiles.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sortOrder) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        items(directories, key = { it.uri }) { dir ->
            DirectoryItem(
                directory = dir, 
                isSelected = selectedItems.contains(dir.uri),
                isCut = cutItemUris.contains(dir.uri),
                appGradient = appGradient,
                onClick = { 
                    if (selectedItems.isNotEmpty()) viewModel.toggleSelection(dir.uri)
                    else viewModel.navigateToDirectory(dir.uri)
                },
                onLongClick = { viewModel.toggleSelection(dir.uri) },
                onInfo = { onShowInfo(dir) }
            )
        }
        items(files, key = { it.uri }) { file ->
            FileItem(
                file = file, 
                isSelected = selectedItems.contains(file.uri),
                isCut = cutItemUris.contains(file.uri),
                appGradient = appGradient,
                onClick = { 
                    if (selectedItems.isNotEmpty()) viewModel.toggleSelection(file.uri)
                    else viewModel.openFile(file.uri, file.mimeType)
                },
                onLongClick = { viewModel.toggleSelection(file.uri) },
                onInfo = { onShowInfo(file) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectoryItem(
    directory: DirectoryData,
    isSelected: Boolean,
    isCut: Boolean,
    appGradient: Brush,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .alpha(if (isCut) 0.5f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = if (isSelected) BorderStroke(2.dp, appGradient) else null
    ) {
        ListItem(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            headlineContent = { Text(directory.name) },
            supportingContent = { 
                val supportText = if (directory.formattedSize.isNotEmpty()) {
                    "${directory.formattedSize} | ${directory.formattedDate}" 
                } else {
                    directory.formattedDate
                }
                Text(supportText)
            },
            leadingContent = { Icon(Icons.Default.Folder, contentDescription = "Directory") },
            trailingContent = {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: FileData,
    isSelected: Boolean,
    isCut: Boolean,
    appGradient: Brush,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .alpha(if (isCut) 0.5f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = if (isSelected) BorderStroke(2.dp, appGradient) else null
    ) {
        ListItem(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            headlineContent = {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = { Text("${file.formattedSize} | ${file.formattedDate}") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Article, "File") },
            trailingContent = {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerTopAppBar(
    currentDirectory: Uri,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onShowCreateFolderDialog: () -> Unit,
    onShowSearch: () -> Unit,
    isClipboardEmpty: Boolean,
    onPaste: () -> Unit,
    onTitleClick: () -> Unit
) {
    val context = LocalContext.current
    val directoryName = remember(currentDirectory) {
        try {
            if (currentDirectory.scheme == "file") {
                currentDirectory.lastPathSegment ?: "Internal Storage"
            } else {
                DocumentFile.fromTreeUri(context, currentDirectory)?.name ?: "File Manager"
            }
        } catch (e: Exception) {
            "File Manager"
        }
    }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AioTopBar(
            windowInsets = WindowInsets.statusBars,
            title = {
                Text(
                    text = "File Manager",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily(Font(R.font.roboto_mono_variable_font_wght))
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = directoryName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTitleClick() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onPaste, enabled = !isClipboardEmpty) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                }
                IconButton(onClick = onShowCreateFolderDialog) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create folder")
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { onSetSortOrder(SortOrder.NAME_AZ); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { onSetSortOrder(SortOrder.NAME_ZA); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Size (Smaller)") }, onClick = { onSetSortOrder(SortOrder.SIZE_SMALLER); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Size (Larger)") }, onClick = { onSetSortOrder(SortOrder.SIZE_LARGER); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Date (Recent)") }, onClick = { onSetSortOrder(SortOrder.DATE_RECENT); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Date (Older)") }, onClick = { onSetSortOrder(SortOrder.DATE_OLDER); showSortMenu = false })
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit, appGradient: Brush, titleFontFamily: FontFamily) {
    var fileName by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isFormComplete = fileName.isNotBlank()

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
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create File",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = titleFontFamily),
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, appGradient, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("File name") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp).width(100.dp)) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontFamily = titleFontFamily)
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

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
                        ) { onCreate(fileName); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Create",
                        color = if (isFormComplete) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit, appGradient: Brush, titleFontFamily: FontFamily) {
    var folderName by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isFormComplete = folderName.isNotBlank()

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
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create Folder",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = titleFontFamily),
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, appGradient, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                TextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp).width(100.dp)) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontFamily = titleFontFamily)
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

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
                        ) { onCreate(folderName); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Create",
                        color = if (isFormComplete) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete the selected item(s)? This action cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.example.aioapp.ui.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    navController: NavController,
    viewModel: FileManagerViewModel = viewModel()
) {
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val canNavigateUp by viewModel.canNavigateUp.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardItem by viewModel.clipboardItem.collectAsState()
    val cutItemUri by viewModel.cutItemUri.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf<Uri?>(null) }

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

    BackHandler(enabled = canNavigateUp) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            currentDirectory?.let { directory ->
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
                    isClipboardEmpty = clipboardItem == null,
                    onPaste = viewModel::paste,
                    onChangeRoot = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        } else {
                            null
                        }
                        if (intent != null) {
                            launcher.launch(intent)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentDirectory != null) {
                FloatingActionButton(onClick = { showCreateFileDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create file")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (currentDirectory != null) {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh() }) {
                    DirectoryContent(
                        viewModel = viewModel,
                        cutItemUri = cutItemUri,
                        onCopy = viewModel::copy,
                        onCut = viewModel::cut,
                        onDelete = { showDeleteConfirmationDialog = it }
                    )
                }
            } else {
                PermissionRequestScreen {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        launcher.launch(intent)
                    } else {
                        // Fallback or legacy permission request could be added here
                        Toast.makeText(context, "All files access is required.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (showCreateFileDialog) {
        CreateFileDialog(onDismiss = { showCreateFileDialog = false }, onCreate = viewModel::createFile)
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(onDismiss = { showCreateFolderDialog = false }, onCreate = viewModel::createFolder)
    }

    showDeleteConfirmationDialog?.let { uri ->
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.delete(uri)
                showDeleteConfirmationDialog = null
            },
            onDismiss = { showDeleteConfirmationDialog = null }
        )
    }
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
        Text("Permission Required", textAlign = TextAlign.Center)
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
    cutItemUri: Uri?,
    onCopy: (Uri) -> Unit,
    onCut: (Uri) -> Unit,
    onDelete: (Uri) -> Unit
) {
    val directories by viewModel.sortedDirectories.collectAsState()
    val files by viewModel.sortedFiles.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(directories) { dir ->
            DirectoryItem(directory = dir, cut = cutItemUri == dir.uri, onClick = { viewModel.navigateToDirectory(dir.uri) }, onCopy = { onCopy(dir.uri) }, onCut = { onCut(dir.uri) }, onDelete = { onDelete(dir.uri) })
        }
        items(files) { file ->
            FileItem(file = file, cut = cutItemUri == file.uri, onClick = { viewModel.openFile(file.uri, file.mimeType) }, onCopy = { onCopy(file.uri) }, onCut = { onCut(file.uri) }, onDelete = { onDelete(file.uri) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectoryItem(
    directory: DirectoryData,
    cut: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
            .alpha(if (cut) 0.5f else 1f),
        headlineContent = { Text(directory.name) },
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = "Directory") },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Copy") }, onClick = {
                        onCopy()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Cut") }, onClick = {
                        onCut()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        onDelete()
                        showMenu = false
                    })
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: FileData,
    cut: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val formattedSize = remember(file.size) { Formatter.formatShortFileSize(context, file.size) }
    val formattedDate = remember(file.lastModified) { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.lastModified)) }

    ListItem(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
            .alpha(if (cut) 0.5f else 1f),
        headlineContent = {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { Text("$formattedSize | $formattedDate") },
        leadingContent = { Icon(Icons.AutoMirrored.Filled.Article, "File") },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Copy") }, onClick = {
                        onCopy()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Cut") }, onClick = {
                        onCut()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        onDelete()
                        showMenu = false
                    })
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerTopAppBar(
    currentDirectory: Uri,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onShowCreateFolderDialog: () -> Unit,
    isClipboardEmpty: Boolean,
    onPaste: () -> Unit,
    onChangeRoot: () -> Unit
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
    var showMoreMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(directoryName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate up")
            }
        },
        actions = {
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
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(text = { Text("Change permission settings") }, onClick = {
                        onChangeRoot()
                        showMoreMenu = false
                    })
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var fileName by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create File") },
        text = {
            TextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(fileName); onDismiss() }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var folderName by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            TextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(folderName); onDismiss() }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
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

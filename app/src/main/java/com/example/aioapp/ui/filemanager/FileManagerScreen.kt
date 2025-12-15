package com.example.aioapp.ui.filemanager

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            viewModel.onDirectorySelected(uri)
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
            FileManagerTopAppBar(
                canNavigateUp = canNavigateUp,
                onNavigateUp = viewModel::navigateUp,
                onSetSortOrder = viewModel::setSortOrder,
                onShowCreateFolderDialog = { showCreateFolderDialog = true },
                isClipboardEmpty = clipboardItem == null,
                onPaste = viewModel::paste
            )
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
            if (currentDirectory == null) {
                Button(onClick = { launcher.launch(null) }) {
                    Text("Select a directory to continue")
                }
            } else {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh() }) {
                    DirectoryContent(
                        viewModel = viewModel,
                        cutItemUri = cutItemUri,
                        onCopy = viewModel::copy,
                        onCut = viewModel::cut,
                        onDelete = { showDeleteConfirmationDialog = it }
                    )
                }
            }

            if (showCreateFileDialog) {
                CreateDialog(
                    title = "Create File",
                    onDismiss = { showCreateFileDialog = false },
                    onCreate = { fileName ->
                        viewModel.createFile(fileName)
                        showCreateFileDialog = false
                    }
                )
            }

            if (showCreateFolderDialog) {
                CreateDialog(
                    title = "Create Folder",
                    onDismiss = { showCreateFolderDialog = false },
                    onCreate = { folderName ->
                        viewModel.createFolder(folderName)
                        showCreateFolderDialog = false
                    }
                )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerTopAppBar(
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onShowCreateFolderDialog: () -> Unit,
    isClipboardEmpty: Boolean,
    onPaste: () -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("File Manager") },
        navigationIcon = {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate up")
                }
            }
        },
        actions = {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort options")
            }
            SortMenu(showSortMenu, { showSortMenu = false }) { sortOrder ->
                onSetSortOrder(sortOrder)
                showSortMenu = false
            }

            IconButton(onClick = { showOptionsMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            OptionsMenu(
                expanded = showOptionsMenu,
                onDismiss = { showOptionsMenu = false },
                onCreateFolder = {
                    onShowCreateFolderDialog()
                    showOptionsMenu = false
                },
                isClipboardEmpty = isClipboardEmpty,
                onPaste = {
                    onPaste()
                    showOptionsMenu = false
                }
            )
        }
    )
}

@Composable
fun DirectoryContent(
    viewModel: FileManagerViewModel,
    cutItemUri: Uri?,
    onCopy: (Uri) -> Unit,
    onCut: (Uri) -> Unit,
    onDelete: (Uri) -> Unit
) {
    val directories by viewModel.directories.collectAsState()
    val files by viewModel.sortedFiles.collectAsState()

    if (directories.isEmpty() && files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This directory is empty.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(directories, key = { it.uri }) { directory ->
                DirectoryItem(
                    name = directory.name,
                    uri = directory.uri,
                    onDirectoryClick = viewModel::navigateToDirectory,
                    isCut = directory.uri == cutItemUri,
                    onCopy = { onCopy(directory.uri) },
                    onCut = { onCut(directory.uri) },
                    onDelete = { onDelete(directory.uri) }
                )
            }
            items(files, key = { it.uri }) { file ->
                FileItem(
                    file = file,
                    isCut = file.uri == cutItemUri,
                    onCopy = { onCopy(file.uri) },
                    onCut = { onCut(file.uri) },
                    onDelete = { onDelete(file.uri) },
                    onFileClick = { viewModel.openFile(file.uri, file.mimeType) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectoryItem(
    name: String,
    uri: Uri,
    onDirectoryClick: (Uri) -> Unit,
    isCut: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onDirectoryClick(uri) },
                    onLongClick = { showMenu = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Directory",
                modifier = Modifier.alpha(if (isCut) 0.5f else 1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, fontSize = 18.sp)
        }
        ItemContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onCopy = {
                onCopy()
                showMenu = false
            },
            onCut = {
                onCut()
                showMenu = false
            },
            onDelete = {
                onDelete()
                showMenu = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: FileData,
    isCut: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onFileClick: (Uri) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onFileClick(file.uri) },
                    onLongClick = { showMenu = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconAlpha = if (isCut) 0.5f else 1f
            if (file.mimeType?.startsWith("image/") == true) {
                AsyncImage(
                    model = file.uri,
                    contentDescription = "Image preview",
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(iconAlpha)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Article,
                    contentDescription = "File",
                    modifier = Modifier.alpha(iconAlpha)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = file.name, fontSize = 18.sp)
        }
        ItemContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onCopy = {
                onCopy()
                showMenu = false
            },
            onCut = {
                onCut()
                showMenu = false
            },
            onDelete = {
                onDelete()
                showMenu = false
            }
        )
    }
}

@Composable
fun CreateDialog(
    title: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) })
            {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun SortMenu(expanded: Boolean, onDismiss: () -> Unit, onSortSelected: (SortOrder) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { onSortSelected(SortOrder.NAME_AZ) })
        DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { onSortSelected(SortOrder.NAME_ZA) })
        DropdownMenuItem(text = { Text("Size (Smaller)") }, onClick = { onSortSelected(SortOrder.SIZE_SMALLER) })
        DropdownMenuItem(text = { Text("Size (Larger)") }, onClick = { onSortSelected(SortOrder.SIZE_LARGER) })
        DropdownMenuItem(text = { Text("Date (Recent)") }, onClick = { onSortSelected(SortOrder.DATE_RECENT) })
        DropdownMenuItem(text = { Text("Date (Older)") }, onClick = { onSortSelected(SortOrder.DATE_OLDER) })
    }
}

@Composable
fun OptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit,
    isClipboardEmpty: Boolean,
    onPaste: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Create Folder") }, onClick = onCreateFolder)
        if (!isClipboardEmpty) {
            DropdownMenuItem(
                text = { Text("Paste") },
                onClick = onPaste,
                leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = "Paste") }
            )
        }
    }
}

@Composable
fun ItemContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = onCopy,
            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
        )
        DropdownMenuItem(
            text = { Text("Cut") },
            onClick = onCut,
            leadingIcon = { Icon(Icons.Filled.ContentCut, contentDescription = "Cut") }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = onDelete,
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete file") },
        text = { Text("Are you sure you want to delete this file? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

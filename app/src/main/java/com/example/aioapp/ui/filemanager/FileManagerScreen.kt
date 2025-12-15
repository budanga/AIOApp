package com.example.aioapp.ui.filemanager

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel = viewModel()
) {
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val canNavigateUp by viewModel.canNavigateUp.collectAsState()
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            viewModel.onDirectorySelected(uri)
        }
    )

    BackHandler(enabled = canNavigateUp) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Manager") },
                navigationIcon = {
                    if (canNavigateUp) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate up")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort options")
                    }
                    SortMenu(showSortMenu, { showSortMenu = false }) { sortOrder ->
                        viewModel.setSortOrder(sortOrder)
                        showSortMenu = false
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    OptionsMenu(showOptionsMenu, { showOptionsMenu = false }) {
                        showCreateFolderDialog = true
                        showOptionsMenu = false
                    }
                }
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
                DirectoryContent(viewModel)
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
        }
    }
}

@Composable
fun DirectoryContent(viewModel: FileManagerViewModel) {
    val directories by viewModel.directories.collectAsState()
    val files by viewModel.sortedFiles.collectAsState()

    if (directories.isEmpty() && files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This directory is empty.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(directories, key = { it.uri }) { directory ->
                DirectoryItem(directory = directory, onClick = { viewModel.navigateToDirectory(directory.uri) })
            }
            items(files, key = { it.uri }) { file ->
                FileItem(file = file)
            }
        }
    }
}

@Composable
fun DirectoryItem(directory: DirectoryData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = "Directory")
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = directory.name, fontSize = 18.sp)
    }
}

@Composable
fun FileItem(file: FileData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.mimeType?.startsWith("image/") == true) {
            AsyncImage(
                model = file.uri,
                contentDescription = "Image preview",
                modifier = Modifier.size(40.dp)
            )
        } else {
            Icon(Icons.Default.Article, contentDescription = "File")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = file.name, fontSize = 18.sp)
    }
}

@Composable
fun CreateDialog(
    title: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
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
fun OptionsMenu(expanded: Boolean, onDismiss: () -> Unit, onCreateFolder: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Create Folder") }, onClick = onCreateFolder)
    }
}

package com.example.aioapp.ui.filemanager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.Stack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileData(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

data class DirectoryData(
    val uri: Uri,
    val name: String
)

enum class SortOrder {
    NAME_AZ,
    NAME_ZA,
    SIZE_SMALLER,
    SIZE_LARGER,
    DATE_RECENT,
    DATE_OLDER
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("file_manager_prefs", Context.MODE_PRIVATE)
    private val directoryStack = Stack<Uri>()

    private val _currentDirectory = MutableStateFlow<Uri?>(null)
    val currentDirectory: StateFlow<Uri?> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileData>>(emptyList())
    private val _directories = MutableStateFlow<List<DirectoryData>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_AZ)

    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val sortedFiles: StateFlow<List<FileData>> = combine(_files, _sortOrder) { files, sortOrder ->
        sortFiles(files, sortOrder)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val directories: StateFlow<List<DirectoryData>> = _directories.asStateFlow()
    val canNavigateUp: StateFlow<Boolean> = MutableStateFlow(false)

    init {
        val uriString = prefs.getString("root_uri", null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val contentResolver = getApplication<Application>().contentResolver
            val persistedUris = contentResolver.persistedUriPermissions
            if (persistedUris.any { it.uri == uri && it.isReadPermission && it.isWritePermission }) {
                _currentDirectory.value = uri
                loadDirectoryContents(uri)
                (canNavigateUp as MutableStateFlow).value = false
            } else {
                prefs.edit().remove("root_uri").apply()
            }
        }
    }

    fun onDirectorySelected(uri: Uri?) {
        uri?.let {
            val contentResolver = getApplication<Application>().contentResolver
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.edit().putString("root_uri", it.toString()).apply()
            directoryStack.clear()
            navigateToDirectory(it)
        }
    }

    fun navigateToDirectory(directoryUri: Uri) {
        directoryStack.push(_currentDirectory.value)
        _currentDirectory.value = directoryUri
        loadDirectoryContents(directoryUri)
        (canNavigateUp as MutableStateFlow).value = true
    }

    fun navigateUp() {
        if (directoryStack.isNotEmpty()) {
            val upUri = directoryStack.pop()
            _currentDirectory.value = upUri
            loadDirectoryContents(upUri)
            (canNavigateUp as MutableStateFlow).value = directoryStack.isNotEmpty()
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    private fun loadDirectoryContents(directoryUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val documentFile = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                if (documentFile == null || !documentFile.isDirectory) {
                    return@withContext
                }

                val files = mutableListOf<FileData>()
                val directories = mutableListOf<DirectoryData>()

                documentFile.listFiles().forEach { file ->
                    if (file.isDirectory) {
                        directories.add(DirectoryData(file.uri, file.name ?: ""))
                    } else {
                        files.add(FileData(file.uri, file.name ?: "", file.length(), file.lastModified(), file.type))
                    }
                }

                _files.value = files
                _directories.value = directories.sortedBy { it.name }
            }
        }
    }

    private fun sortFiles(files: List<FileData>, sortOrder: SortOrder): List<FileData> {
        return when (sortOrder) {
            SortOrder.NAME_AZ -> files.sortedBy { it.name }
            SortOrder.NAME_ZA -> files.sortedByDescending { it.name }
            SortOrder.SIZE_SMALLER -> files.sortedBy { it.size }
            SortOrder.SIZE_LARGER -> files.sortedByDescending { it.size }
            SortOrder.DATE_RECENT -> files.sortedByDescending { it.lastModified }
            SortOrder.DATE_OLDER -> files.sortedBy { it.lastModified }
        }
    }

    fun createFile(fileName: String) {
        val directoryUri = _currentDirectory.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                directory?.createFile("text/plain", fileName)
                loadDirectoryContents(directoryUri) // Refresh list
            }
        }
    }

    fun createFolder(folderName: String) {
        val directoryUri = _currentDirectory.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                directory?.createDirectory(folderName)
                loadDirectoryContents(directoryUri) // Refresh list
            }
        }
    }
}

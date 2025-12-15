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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val directoryContentCache = mutableMapOf<Uri, Pair<List<DirectoryData>, List<FileData>>>()
    private var directoryLoadingJob: Job? = null

    private val _currentDirectory = MutableStateFlow<Uri?>(null)
    val currentDirectory: StateFlow<Uri?> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileData>>(emptyList())
    private val _directories = MutableStateFlow<List<DirectoryData>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_AZ)
    private val _toastMessage = MutableSharedFlow<String>()
    private val _isRefreshing = MutableStateFlow(false)
    private val _canNavigateUp = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    val directories: StateFlow<List<DirectoryData>> = _directories.asStateFlow()
    val canNavigateUp: StateFlow<Boolean> = _canNavigateUp.asStateFlow()

    val sortedFiles: StateFlow<List<FileData>> = combine(_files, _sortOrder) { files, sortOrder ->
        sortFiles(files, sortOrder)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val uriString = prefs.getString("root_uri", null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val contentResolver = getApplication<Application>().contentResolver
            val persistedUris = contentResolver.persistedUriPermissions
            if (persistedUris.any { it.uri == uri && it.isReadPermission && it.isWritePermission }) {
                _currentDirectory.value = uri
                loadDirectoryContents(uri)
                _canNavigateUp.value = false
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
            directoryContentCache.clear()
            navigateToDirectory(it)
        }
    }

    fun navigateToDirectory(directoryUri: Uri) {
        _currentDirectory.value?.let { directoryStack.push(it) }
        _currentDirectory.value = directoryUri
        loadDirectoryContents(directoryUri)
        _canNavigateUp.value = true
    }

    fun navigateUp() {
        if (directoryStack.isNotEmpty()) {
            val upUri = directoryStack.pop()
            _currentDirectory.value = upUri
            loadDirectoryContents(upUri)
            _canNavigateUp.value = directoryStack.isNotEmpty()
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun refresh() {
        directoryLoadingJob?.cancel()
        val currentUri = _currentDirectory.value ?: return
        directoryLoadingJob = viewModelScope.launch {
            _isRefreshing.value = true
            directoryContentCache.remove(currentUri)
            loadDirectoryContentsInternal(currentUri)
            _isRefreshing.value = false
        }
    }

    private suspend fun loadDirectoryContentsInternal(directoryUri: Uri) {
        if (directoryContentCache.containsKey(directoryUri)) {
            val (dirs, files) = directoryContentCache.getValue(directoryUri)
            if (directoryUri == _currentDirectory.value) {
                _directories.value = dirs
                _files.value = files
            }
            return
        }

        withContext(Dispatchers.IO) {
            if (directoryUri != _currentDirectory.value) return@withContext

            val documentFile = DocumentFile.fromTreeUri(getApplication(), directoryUri)
            if (documentFile == null || !documentFile.isDirectory) {
                if (directoryUri == _currentDirectory.value) {
                    _files.value = emptyList()
                    _directories.value = emptyList()
                }
                return@withContext
            }

            val allDocs = documentFile.listFiles().toList()
            val (directoryDocs, fileDocs) = allDocs.partition { it.isDirectory }

            val directoryData = directoryDocs.map { DirectoryData(it.uri, it.name ?: "") }.sortedBy { it.name }

            if (directoryUri != _currentDirectory.value) return@withContext

            _directories.value = directoryData
            _files.value = emptyList()

            val allFilesData = mutableListOf<FileData>()
            run loop@{
                fileDocs.chunked(30).forEach { chunk ->
                    if (directoryUri != _currentDirectory.value) return@loop

                    val fileDataChunk = chunk.map { FileData(it.uri, it.name ?: "", it.length(), it.lastModified(), it.type) }
                    if (directoryUri == _currentDirectory.value) {
                        _files.value = _files.value + fileDataChunk
                    }
                    allFilesData.addAll(fileDataChunk)
                }
            }

            if (directoryUri == _currentDirectory.value) {
                directoryContentCache[directoryUri] = directoryData to allFilesData
            }
        }
    }

    private fun loadDirectoryContents(directoryUri: Uri) {
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            loadDirectoryContentsInternal(directoryUri)
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

    private fun invalidateCacheForCurrentDir() {
        currentDirectory.value?.let { directoryContentCache.remove(it) }
    }

    fun createFile(fileName: String) {
        if (fileName.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("File name cannot be empty.") }
            return
        }

        if (_files.value.any { it.name.equals(fileName, ignoreCase = true) } || _directories.value.any { it.name.equals(fileName, ignoreCase = true) }) {
            viewModelScope.launch { _toastMessage.emit("A file or folder with this name already exists.") }
            return
        }

        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                directory?.createFile("text/plain", fileName)
            }
            invalidateCacheForCurrentDir()
            loadDirectoryContentsInternal(directoryUri)
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Folder name cannot be empty.") }
            return
        }

        if (_files.value.any { it.name.equals(folderName, ignoreCase = true) } || _directories.value.any { it.name.equals(folderName, ignoreCase = true) }) {
            viewModelScope.launch { _toastMessage.emit("A file or folder with this name already exists.") }
            return
        }

        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                directory?.createDirectory(folderName)
            }
            invalidateCacheForCurrentDir()
            loadDirectoryContentsInternal(directoryUri)
        }
    }
}

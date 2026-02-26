package com.example.aioapp.ui.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

data class ClipboardItem(val uri: Uri, val action: ClipboardAction)

enum class ClipboardAction {
    COPY, CUT
}

enum class SortOrder {
    NAME_AZ,
    NAME_ZA,
    SIZE_SMALLER,
    SIZE_LARGER,
    DATE_RECENT,
    DATE_OLDER
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val directoryContentCache = mutableMapOf<Uri, Pair<List<DirectoryData>, List<FileData>>>()
    private var directoryLoadingJob: Job? = null

    private val _directoryStack = MutableStateFlow<List<Uri>>(emptyList())
    private val _currentDirectory = MutableStateFlow<Uri?>(null)
    private val _files = MutableStateFlow<List<FileData>>(emptyList())
    private val _directories = MutableStateFlow<List<DirectoryData>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_AZ)
    private val _toastMessage = MutableSharedFlow<String>()
    private val _isRefreshing = MutableStateFlow(false)
    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)

    val currentDirectory: StateFlow<Uri?> = _currentDirectory.asStateFlow()
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val canNavigateUp: StateFlow<Boolean> = _directoryStack
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cutItemUri: StateFlow<Uri?> = clipboardItem
        .map { if (it?.action == ClipboardAction.CUT) it.uri else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sortedFiles: StateFlow<List<FileData>> = combine(_files, _sortOrder) { files, sortOrder ->
        sortFiles(files, sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedDirectories: StateFlow<List<DirectoryData>> = combine(_directories, _sortOrder) { dirs, order ->
        sortDirectories(dirs, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissionsAndLoad()
    }

    fun checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (_currentDirectory.value == null) {
                val rootUri = Uri.fromFile(Environment.getExternalStorageDirectory())
                _currentDirectory.value = rootUri
                loadDirectoryContents(rootUri)
            }
        } else {
            _currentDirectory.value = null
        }
    }

    fun navigateToDirectory(directoryUri: Uri) {
        _currentDirectory.value?.let { _directoryStack.value += it }
        _currentDirectory.value = directoryUri
        loadDirectoryContents(directoryUri)
    }

    fun navigateUp() {
        val stack = _directoryStack.value
        if (stack.isNotEmpty()) {
            _currentDirectory.value = stack.last()
            _directoryStack.value = stack.dropLast(1)
            loadDirectoryContents(stack.last())
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun refresh() {
        val currentUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            _isRefreshing.value = true
            directoryContentCache.remove(currentUri)
            loadDirectoryContentsInternal(currentUri)
            _isRefreshing.value = false
        }
    }

    fun createFile(fileName: String) {
        if (fileName.isBlank()) {
            showToast("File name cannot be empty.")
            return
        }
        performFileOperation { directoryUri ->
            if (directoryUri.scheme == "file") {
                File(directoryUri.path, fileName).createNewFile()
            } else {
                DocumentFile.fromTreeUri(getApplication(), directoryUri)?.createFile("text/plain", fileName)
            }
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) {
            showToast("Folder name cannot be empty.")
            return
        }
        performFileOperation { directoryUri ->
            if (directoryUri.scheme == "file") {
                File(directoryUri.path, folderName).mkdirs()
            } else {
                DocumentFile.fromTreeUri(getApplication(), directoryUri)?.createDirectory(folderName)
            }
        }
    }

    fun copy(uri: Uri) {
        _clipboardItem.value = ClipboardItem(uri, ClipboardAction.COPY)
        showToast("Copied to clipboard")
    }

    fun cut(uri: Uri) {
        _clipboardItem.value = ClipboardItem(uri, ClipboardAction.CUT)
        showToast("Cut to clipboard")
    }

    fun delete(uri: Uri) {
        viewModelScope.launch {
            val (success, message) = withContext(Dispatchers.IO) {
                try {
                    val deleted = if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    } else {
                        DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, uri)
                    }
                    deleted to if (deleted) "Deleted successfully." else "Failed to delete item."
                } catch (e: Exception) {
                    false to "Error: ${e.message}"
                }
            }
            _toastMessage.emit(message)
            if (success) {
                invalidateCacheAndReload()
            }
        }
    }

    fun paste() {
        val itemToPaste = _clipboardItem.value ?: return
        val destinationDirUri = _currentDirectory.value ?: return

        viewModelScope.launch {
            val (success, message) = withContext(Dispatchers.IO) {
                try {
                    if (itemToPaste.uri.scheme == "file" && destinationDirUri.scheme == "file") {
                        val sourceFile = File(itemToPaste.uri.path ?: "")
                        val destFile = File(destinationDirUri.path ?: "", sourceFile.name)
                        
                        val result = when (itemToPaste.action) {
                            ClipboardAction.COPY -> {
                                if (sourceFile.isDirectory) {
                                    sourceFile.copyRecursively(destFile, overwrite = false)
                                } else {
                                    sourceFile.copyTo(destFile, overwrite = false)
                                    true
                                }
                            }
                            ClipboardAction.CUT -> sourceFile.renameTo(destFile)
                        }
                        result to if (result) "Pasted successfully." else "Paste failed (file might already exist)."
                    } else {
                        false to "Pasting across different storage types not fully supported."
                    }
                } catch (e: Exception) {
                    false to "Error: ${e.message}"
                }
            }
            _toastMessage.emit(message)
            if (success) {
                if (itemToPaste.action == ClipboardAction.CUT) _clipboardItem.value = null
                invalidateCacheAndReload()
            }
        }
    }

    fun openFile(uri: Uri, mimeType: String?) {
        val context = getApplication<Application>().applicationContext
        val contentUri = if (uri.scheme == "file") {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(uri.path ?: ""))
        } else {
            uri
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("No app found to open this file.")
        }
    }

    private fun loadDirectoryContents(directoryUri: Uri) {
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            loadDirectoryContentsInternal(directoryUri)
        }
    }

    private suspend fun loadDirectoryContentsInternal(directoryUri: Uri) {
        directoryContentCache[directoryUri]?.let { (dirs, files) ->
            if (directoryUri == _currentDirectory.value) {
                _directories.value = dirs
                _files.value = files
            }
            return
        }

        withContext(Dispatchers.IO) {
            if (directoryUri != _currentDirectory.value) return@withContext

            if (directoryUri.scheme == "file") {
                loadFromFileSystem(directoryUri)
            } else {
                loadFromDocumentFile(directoryUri)
            }
        }
    }

    private fun loadFromFileSystem(directoryUri: Uri) {
        val file = File(directoryUri.path ?: "")
        if (!file.exists() || !file.isDirectory) return

        val allFiles = file.listFiles()?.toList() ?: emptyList()
        val (directoryFiles, regularFiles) = allFiles.partition { it.isDirectory }

        val directoryData = directoryFiles.map { DirectoryData(Uri.fromFile(it), it.name) }
        val fileData = regularFiles.map { 
            FileData(Uri.fromFile(it), it.name, it.length(), it.lastModified(), getMimeType(it)) 
        }

        if (directoryUri == _currentDirectory.value) {
            _directories.value = directoryData
            _files.value = fileData
            directoryContentCache[directoryUri] = directoryData to fileData
        }
    }

    private fun loadFromDocumentFile(directoryUri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(getApplication(), directoryUri)
        if (documentFile == null || !documentFile.isDirectory) {
            if (directoryUri == _currentDirectory.value) {
                _files.value = emptyList()
                _directories.value = emptyList()
            }
            return
        }

        val allDocs = documentFile.listFiles().toList()
        val (directoryDocs, fileDocs) = allDocs.partition { it.isDirectory }

        val directoryData = directoryDocs.map { DirectoryData(it.uri, it.name ?: "") }
        val fileData = mutableListOf<FileData>()

        if (directoryUri == _currentDirectory.value) {
            _directories.value = directoryData
            _files.value = emptyList()
        }

        fileDocs.chunked(30).forEach { chunk ->
            if (directoryUri != _currentDirectory.value) return@forEach
            val fileDataChunk = chunk.map { 
                FileData(it.uri, it.name ?: "", it.length(), it.lastModified(), it.type) 
            }
            fileData.addAll(fileDataChunk)
            if (directoryUri == _currentDirectory.value) {
                _files.value += fileDataChunk
            }
        }

        if (directoryUri == _currentDirectory.value) {
            directoryContentCache[directoryUri] = directoryData to fileData
        }
    }

    private fun getMimeType(file: File): String? =
        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)

    private fun sortFiles(files: List<FileData>, sortOrder: SortOrder): List<FileData> = when (sortOrder) {
        SortOrder.NAME_AZ -> files.sortedBy { it.name }
        SortOrder.NAME_ZA -> files.sortedByDescending { it.name }
        SortOrder.SIZE_SMALLER -> files.sortedBy { it.size }
        SortOrder.SIZE_LARGER -> files.sortedByDescending { it.size }
        SortOrder.DATE_RECENT -> files.sortedByDescending { it.lastModified }
        SortOrder.DATE_OLDER -> files.sortedBy { it.lastModified }
    }

    private fun sortDirectories(directories: List<DirectoryData>, sortOrder: SortOrder): List<DirectoryData> = 
        when (sortOrder) {
            SortOrder.NAME_AZ -> directories.sortedBy { it.name }
            SortOrder.NAME_ZA -> directories.sortedByDescending { it.name }
            else -> directories
        }

    private fun performFileOperation(operation: suspend (Uri) -> Unit) {
        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                operation(directoryUri)
            }
            invalidateCacheAndReload()
        }
    }

    private suspend fun invalidateCacheAndReload() {
        _currentDirectory.value?.let { currentUri ->
            directoryContentCache.remove(currentUri)
            loadDirectoryContentsInternal(currentUri)
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch { _toastMessage.emit(message) }
    }
}

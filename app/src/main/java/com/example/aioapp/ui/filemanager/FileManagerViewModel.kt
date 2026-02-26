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

    private val _directoryStack = MutableStateFlow<List<Uri>>(emptyList())
    private val directoryContentCache = mutableMapOf<Uri, Pair<List<DirectoryData>, List<FileData>>>()
    private var directoryLoadingJob: Job? = null

    private val _currentDirectory = MutableStateFlow<Uri?>(null)
    val currentDirectory: StateFlow<Uri?> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileData>>(emptyList())
    private val _directories = MutableStateFlow<List<DirectoryData>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_AZ)
    private val _toastMessage = MutableSharedFlow<String>()
    private val _isRefreshing = MutableStateFlow(false)

    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val canNavigateUp: StateFlow<Boolean> = _directoryStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cutItemUri: StateFlow<Uri?> = clipboardItem.map {
        if (it?.action == ClipboardAction.CUT) it.uri else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                val rootFile = Environment.getExternalStorageDirectory()
                val rootUri = Uri.fromFile(rootFile)
                if (_currentDirectory.value == null) {
                    _currentDirectory.value = rootUri
                    loadDirectoryContents(rootUri)
                }
            } else {
                _currentDirectory.value = null
            }
        } else {
            // For older versions, you'd check standard storage permissions
            _currentDirectory.value = null
        }
    }

    fun onRootDirectorySelected(uri: Uri?) {
        checkPermissionsAndLoad()
    }

    fun navigateToDirectory(directoryUri: Uri) {
        _currentDirectory.value?.let { _directoryStack.value = _directoryStack.value + it }
        _currentDirectory.value = directoryUri
        loadDirectoryContents(directoryUri)
    }

    fun navigateUp() {
        if (_directoryStack.value.isNotEmpty()) {
            val stack = _directoryStack.value
            val upUri = stack.last()
            _directoryStack.value = stack.dropLast(1)
            _currentDirectory.value = upUri
            loadDirectoryContents(upUri)
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

            val file = if (directoryUri.scheme == "file") {
                File(directoryUri.path ?: "")
            } else {
                null
            }

            if (file != null && file.exists() && file.isDirectory) {
                val allDocs = file.listFiles()?.toList() ?: emptyList()
                val (directoryDocs, fileDocs) = allDocs.partition { it.isDirectory }

                val directoryData = directoryDocs.map { DirectoryData(Uri.fromFile(it), it.name) }.sortedBy { it.name }
                val fileData = fileDocs.map { FileData(Uri.fromFile(it), it.name, it.length(), it.lastModified(), getMimeType(it)) }

                if (directoryUri == _currentDirectory.value) {
                    _directories.value = directoryData
                    _files.value = fileData
                    directoryContentCache[directoryUri] = directoryData to fileData
                }
            } else {
                // Fallback to DocumentFile for non-file URIs if any
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
                _directories.value = directoryData
                _files.value = emptyList()

                val allFilesData = mutableListOf<FileData>()
                fileDocs.chunked(30).forEach { chunk ->
                    if (directoryUri != _currentDirectory.value) return@forEach
                    val fileDataChunk = chunk.map { FileData(it.uri, it.name ?: "", it.length(), it.lastModified(), it.type) }
                    if (directoryUri == _currentDirectory.value) {
                        _files.value = _files.value + fileDataChunk
                    }
                    allFilesData.addAll(fileDataChunk)
                }
                if (directoryUri == _currentDirectory.value) {
                    directoryContentCache[directoryUri] = directoryData to allFilesData
                }
            }
        }
    }

    private fun getMimeType(file: File): String? {
        val extension = file.extension
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
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

    private fun sortDirectories(directories: List<DirectoryData>, sortOrder: SortOrder): List<DirectoryData> {
        return when (sortOrder) {
            SortOrder.NAME_AZ -> directories.sortedBy { it.name }
            SortOrder.NAME_ZA -> directories.sortedByDescending { it.name }
            else -> directories
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

        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (directoryUri.scheme == "file") {
                    val file = File(directoryUri.path, fileName)
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                } else {
                    val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                    directory?.createFile("text/plain", fileName)
                }
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

        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (directoryUri.scheme == "file") {
                    val dir = File(directoryUri.path, folderName)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                } else {
                    val directory = DocumentFile.fromTreeUri(getApplication(), directoryUri)
                    directory?.createDirectory(folderName)
                }
            }
            invalidateCacheForCurrentDir()
            loadDirectoryContentsInternal(directoryUri)
        }
    }

    fun copy(uri: Uri) {
        _clipboardItem.value = ClipboardItem(uri, ClipboardAction.COPY)
        viewModelScope.launch { _toastMessage.emit("Copied to clipboard") }
    }

    fun cut(uri: Uri) {
        _clipboardItem.value = ClipboardItem(uri, ClipboardAction.CUT)
        viewModelScope.launch { _toastMessage.emit("Cut to clipboard") }
    }

    fun delete(uri: Uri) {
        viewModelScope.launch {
            var success = false
            var message = "Failed to delete item."
            withContext(Dispatchers.IO) {
                try {
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.isDirectory) {
                            success = file.deleteRecursively()
                        } else {
                            success = file.delete()
                        }
                        if (success) message = "Deleted successfully."
                    } else {
                        if (DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, uri)) {
                            message = "Deleted successfully."
                            success = true
                        }
                    }
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                }
            }
            _toastMessage.emit(message)
            if (success) {
                invalidateCacheForCurrentDir()
                loadDirectoryContentsInternal(_currentDirectory.value!!)
            }
        }
    }

    fun paste() {
        val itemToPaste = _clipboardItem.value ?: return
        val destinationDirUri = _currentDirectory.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var success = false
                var message = ""
                try {
                    if (itemToPaste.uri.scheme == "file" && destinationDirUri.scheme == "file") {
                        val sourceFile = File(itemToPaste.uri.path ?: "")
                        val destDir = File(destinationDirUri.path ?: "")
                        val destFile = File(destDir, sourceFile.name)

                        if (itemToPaste.action == ClipboardAction.COPY) {
                            if (sourceFile.isDirectory) {
                                success = sourceFile.copyRecursively(destFile, overwrite = false)
                            } else {
                                sourceFile.copyTo(destFile, overwrite = false)
                                success = true
                            }
                        } else {
                            success = sourceFile.renameTo(destFile)
                        }
                        message = if (success) "Pasted successfully." else "Paste failed (file might already exist)."
                    } else {
                        message = "Pasting across different storage types not fully supported."
                    }
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                }

                _toastMessage.emit(message)
                if (success) {
                    if (itemToPaste.action == ClipboardAction.CUT) _clipboardItem.value = null
                    invalidateCacheForCurrentDir()
                    loadDirectoryContentsInternal(destinationDirUri)
                }
            }
        }
    }

    fun openFile(uri: Uri, mimeType: String?) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val contentUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }
            setDataAndType(contentUri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch { _toastMessage.emit("No app found to open this file.") }
        }
    }

    fun changeRootDirectory() {
        // Now handled by jumping to settings in Screen
    }
}

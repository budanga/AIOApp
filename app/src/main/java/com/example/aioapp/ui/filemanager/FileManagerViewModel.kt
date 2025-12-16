package com.example.aioapp.ui.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
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

    private fun checkPermissionsAndLoad() {
        val contentResolver = getApplication<Application>().contentResolver
        val persistedUris = contentResolver.persistedUriPermissions
        persistedUris.lastOrNull()?.uri?.let {
            _currentDirectory.value = it
            loadDirectoryContents(it)
        } ?: run {
            _currentDirectory.value = null
        }
    }

    fun onRootDirectorySelected(uri: Uri?) {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver

        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            _currentDirectory.value = uri
            loadDirectoryContents(uri)
        } else {
            viewModelScope.launch {
                _toastMessage.emit("Permission to access storage was not granted.")
            }
            _currentDirectory.value = null
        }
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

    private fun sortDirectories(directories: List<DirectoryData>, sortOrder: SortOrder): List<DirectoryData> {
        return when (sortOrder) {
            SortOrder.NAME_AZ -> directories.sortedBy { it.name }
            SortOrder.NAME_ZA -> directories.sortedByDescending { it.name }
            else -> directories // For size/date, keep current order (which is name ascending)
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
            var message = "Failed to delete file."
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>().applicationContext
                try {
                    if (DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                        message = "File deleted successfully."
                        success = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    message = "Failed to delete file: ${e.message}"
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
                val context = getApplication<Application>().applicationContext
                val sourceFile = DocumentFile.fromSingleUri(context, itemToPaste.uri)

                if (sourceFile == null || !sourceFile.exists()) {
                    _toastMessage.emit("Error accessing source file.")
                    return@withContext
                }

                if (destinationDirUri == sourceFile.uri) {
                    _toastMessage.emit("Source and destination cannot be the same.")
                    return@withContext
                }

                if (sourceFile.isDirectory) {
                    var parent = DocumentFile.fromTreeUri(context, destinationDirUri)
                    while (parent != null) {
                        if (parent.uri == sourceFile.uri) {
                            _toastMessage.emit("Cannot paste a directory into its own subdirectory.")
                            return@withContext
                        }
                        parent = parent.parentFile
                    }
                }

                val destinationDir = DocumentFile.fromTreeUri(context, destinationDirUri)
                if (destinationDir == null || !destinationDir.isDirectory) {
                    _toastMessage.emit("Invalid destination directory.")
                    return@withContext
                }

                val existingFile = destinationDir.findFile(sourceFile.name ?: "")
                if (existingFile != null && existingFile.exists()) {
                    _toastMessage.emit("A file with the same name already exists.")
                    return@withContext
                }

                var success = false
                var message = ""

                try {
                    when (itemToPaste.action) {
                        ClipboardAction.COPY -> {
                            val newFileUri = DocumentsContract.copyDocument(context.contentResolver, sourceFile.uri, destinationDir.uri)
                            success = newFileUri != null
                            message = if (success) "File copied successfully." else "File copy failed."
                        }
                        ClipboardAction.CUT -> {
                             val sourceParentUri = sourceFile.parentFile?.uri ?: run {
                                _toastMessage.emit("Cannot move this file or directory.")
                                return@withContext
                            }
                            val movedFileUri = DocumentsContract.moveDocument(context.contentResolver, sourceFile.uri, sourceParentUri, destinationDir.uri)
                            success = movedFileUri != null
                             message = if (success) "File moved successfully." else "File move failed."

                            if(success) {
                                directoryContentCache.remove(sourceParentUri)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback for copy
                    if(itemToPaste.action == ClipboardAction.COPY) {
                        success = copyFileManually(sourceFile, destinationDir)
                        message = if (success) "File copied successfully." else "File copy failed."
                    }
                    // Fallback for cut (copy then delete)
                    else if (itemToPaste.action == ClipboardAction.CUT) {
                        success = copyFileManually(sourceFile, destinationDir)
                        if (success) {
                             if (DocumentsContract.deleteDocument(context.contentResolver, sourceFile.uri)) {
                                 message = "File moved successfully."
                                 sourceFile.parentFile?.uri?.let { directoryContentCache.remove(it) }
                             } else {
                                 message = "Copied, but failed to delete original file."
                             }
                        } else {
                            message = "File move failed."
                        }
                    }
                }

                _toastMessage.emit(message)

                if (success) {
                    if (itemToPaste.action == ClipboardAction.CUT) {
                        _clipboardItem.value = null
                    }
                    invalidateCacheForCurrentDir()
                    loadDirectoryContentsInternal(destinationDirUri)
                }
            }
        }
    }

    private fun copyFileManually(source: DocumentFile, destinationDir: DocumentFile): Boolean {
        val context = getApplication<Application>().applicationContext
        val contentResolver = context.contentResolver

        if (source.isDirectory) {
            val newDir = destinationDir.createDirectory(source.name ?: "New Folder")
            if (newDir == null) {
                return false
            }
            return source.listFiles().all { fileInDir ->
                copyFileManually(fileInDir, newDir)
            }
        } else {
            val newFile = destinationDir.createFile(source.type ?: "application/octet-stream", source.name ?: "new_file")
            if (newFile == null) {
                return false
            }
            return try {
                contentResolver.openInputStream(source.uri)?.use { inputStream ->
                    contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: Exception) {
                newFile.delete()
                false
            }
        }
    }

    fun openFile(uri: Uri, mimeType: String?) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            viewModelScope.launch {
                _toastMessage.emit("No app found to open this file type.")
            }
        }
    }

    fun changeRootDirectory() {
        val contentResolver = getApplication<Application>().contentResolver
        contentResolver.persistedUriPermissions.forEach {
            contentResolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        _currentDirectory.value = null
        _directoryStack.value = emptyList()
        _files.value = emptyList()
        _directories.value = emptyList()
        directoryContentCache.clear()
    }
}

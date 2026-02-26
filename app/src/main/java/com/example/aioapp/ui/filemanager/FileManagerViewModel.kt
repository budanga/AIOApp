package com.example.aioapp.ui.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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

    private val _directoryStack = MutableStateFlow<List<File>>(emptyList())
    private val directoryContentCache = mutableMapOf<File, Pair<List<DirectoryData>, List<FileData>>>()
    private var directoryLoadingJob: Job? = null

    private val _currentDirectory = MutableStateFlow<File?>(null)
    val currentDirectory: StateFlow<File?> = _currentDirectory.asStateFlow()

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
        if (hasAllFilesPermission()) {
            val root = Environment.getExternalStorageDirectory()
            _currentDirectory.value = root
            loadDirectoryContents(root)
        } else {
            _currentDirectory.value = null
        }
    }

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // On older versions, standard permissions are enough (already in manifest)
        }
    }

    fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${getApplication<Application>().packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        }
    }

    fun navigateToDirectory(directoryFile: File) {
        _currentDirectory.value?.let { _directoryStack.value = _directoryStack.value + it }
        _currentDirectory.value = directoryFile
        loadDirectoryContents(directoryFile)
    }

    fun navigateUp() {
        if (_directoryStack.value.isNotEmpty()) {
            val stack = _directoryStack.value
            val upFile = stack.last()
            _directoryStack.value = stack.dropLast(1)
            _currentDirectory.value = upFile
            loadDirectoryContents(upFile)
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun refresh() {
        directoryLoadingJob?.cancel()
        val currentDir = _currentDirectory.value ?: return
        directoryLoadingJob = viewModelScope.launch {
            _isRefreshing.value = true
            directoryContentCache.remove(currentDir)
            loadDirectoryContentsInternal(currentDir)
            _isRefreshing.value = false
        }
    }

    private suspend fun loadDirectoryContentsInternal(directory: File) {
        if (directoryContentCache.containsKey(directory)) {
            val (dirs, files) = directoryContentCache.getValue(directory)
            if (directory == _currentDirectory.value) {
                _directories.value = dirs
                _files.value = files
            }
            return
        }

        withContext(Dispatchers.IO) {
            if (directory != _currentDirectory.value) return@withContext

            if (!directory.exists() || !directory.isDirectory) {
                if (directory == _currentDirectory.value) {
                    _files.value = emptyList()
                    _directories.value = emptyList()
                }
                return@withContext
            }

            val allFiles = directory.listFiles()?.toList() ?: emptyList()
            val (directoryFiles, fileFiles) = allFiles.partition { it.isDirectory }

            val directoryData = directoryFiles.map { DirectoryData(Uri.fromFile(it), it.name) }.sortedBy { it.name }

            if (directory != _currentDirectory.value) return@withContext

            _directories.value = directoryData
            _files.value = emptyList()

            val allFilesData = fileFiles.map { 
                FileData(Uri.fromFile(it), it.name, it.length(), it.lastModified(), getMimeType(it))
            }

            if (directory == _currentDirectory.value) {
                _files.value = allFilesData
                directoryContentCache[directory] = directoryData to allFilesData
            }
        }
    }

    private fun getMimeType(file: File): String? {
        val extension = file.extension
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun loadDirectoryContents(directory: File) {
        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            loadDirectoryContentsInternal(directory)
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
        _currentDirectory.value?.let { directoryContentCache.remove(it) }
    }

    fun createFile(fileName: String) {
        if (fileName.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("File name cannot be empty.") }
            return
        }

        val currentDir = _currentDirectory.value ?: return
        val newFile = File(currentDir, fileName)
        
        if (newFile.exists()) {
            viewModelScope.launch { _toastMessage.emit("A file or folder with this name already exists.") }
            return
        }

        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                newFile.createNewFile()
            }
            invalidateCacheForCurrentDir()
            loadDirectoryContentsInternal(currentDir)
        }
    }

    fun createFolder(folderName: String) {
        if (folderName.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Folder name cannot be empty.") }
            return
        }

        val currentDir = _currentDirectory.value ?: return
        val newFolder = File(currentDir, folderName)

        if (newFolder.exists()) {
            viewModelScope.launch { _toastMessage.emit("A file or folder with this name already exists.") }
            return
        }

        directoryLoadingJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                newFolder.mkdirs()
            }
            invalidateCacheForCurrentDir()
            loadDirectoryContentsInternal(currentDir)
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
            val file = uri.path?.let { File(it) }
            if (file != null && file.exists()) {
                val success = withContext(Dispatchers.IO) {
                    file.deleteRecursively()
                }
                if (success) {
                    _toastMessage.emit("Deleted successfully.")
                    invalidateCacheForCurrentDir()
                    _currentDirectory.value?.let { loadDirectoryContentsInternal(it) }
                } else {
                    _toastMessage.emit("Failed to delete.")
                }
            }
        }
    }

    fun paste() {
        val itemToPaste = _clipboardItem.value ?: return
        val destinationDir = _currentDirectory.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sourceFile = itemToPaste.uri.path?.let { File(it) }
                if (sourceFile == null || !sourceFile.exists()) {
                    _toastMessage.emit("Error accessing source file.")
                    return@withContext
                }

                val targetFile = File(destinationDir, sourceFile.name)
                if (targetFile.exists()) {
                    _toastMessage.emit("A file with the same name already exists.")
                    return@withContext
                }

                try {
                    if (itemToPaste.action == ClipboardAction.COPY) {
                        sourceFile.copyRecursively(targetFile)
                        _toastMessage.emit("Copied successfully.")
                    } else {
                        if (sourceFile.renameTo(targetFile)) {
                            _toastMessage.emit("Moved successfully.")
                            _clipboardItem.value = null
                        } else {
                            sourceFile.copyRecursively(targetFile)
                            sourceFile.deleteRecursively()
                            _toastMessage.emit("Moved successfully.")
                            _clipboardItem.value = null
                        }
                    }
                    invalidateCacheForCurrentDir()
                    loadDirectoryContentsInternal(destinationDir)
                } catch (e: Exception) {
                    _toastMessage.emit("Operation failed: ${e.message}")
                }
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
}

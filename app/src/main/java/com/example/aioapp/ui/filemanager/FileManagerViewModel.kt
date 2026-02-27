package com.example.aioapp.ui.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Stable
data class FileData(
    val uri: Uri,
    val name: String,
    val size: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val creationTime: Long = 0L,
    val mimeType: String?,
    val path: String = ""
)

@Stable
data class DirectoryData(
    val uri: Uri,
    val name: String,
    val size: Long = 0L,
    val formattedSize: String = "",
    val lastModified: Long = 0L,
    val formattedDate: String = "",
    val creationTime: Long = 0L,
    val path: String = ""
)

sealed class SearchResult {
    data class File(val data: FileData) : SearchResult()
    data class Directory(val data: DirectoryData) : SearchResult()
}

data class ClipboardItem(val uris: List<Uri>, val action: ClipboardAction)

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

    companion object {
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun formatDate(timestamp: Long): String {
            return if (timestamp > 0) dateFormatter.format(Date(timestamp)) else ""
        }
    }

    private val directoryContentCache = mutableMapOf<Uri, Pair<List<DirectoryData>, List<FileData>>>()
    private val directorySizeCache = mutableMapOf<Uri, Long>()
    private var directoryLoadingJob: Job? = null
    private var searchJob: Job? = null
    private var sizeCalculationJob: Job? = null

    private val _directoryStack = MutableStateFlow<List<Uri>>(emptyList())
    private val _currentDirectory = MutableStateFlow<Uri?>(null)
    private val _files = MutableStateFlow<List<FileData>>(emptyList())
    private val _directories = MutableStateFlow<List<DirectoryData>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_AZ)
    private val _toastMessage = MutableSharedFlow<String>()
    private val _isRefreshing = MutableStateFlow(false)
    private val _clipboardItem = MutableStateFlow<ClipboardItem?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedItems: StateFlow<Set<Uri>> = _selectedItems.asStateFlow()

    val currentDirectory: StateFlow<Uri?> = _currentDirectory.asStateFlow()
    val clipboardItem: StateFlow<ClipboardItem?> = _clipboardItem.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val canNavigateUp: StateFlow<Boolean> = _directoryStack
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cutItemUris: StateFlow<Set<Uri>> = clipboardItem
        .map { if (it?.action == ClipboardAction.CUT) it.uris.toSet() else emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val sortedFiles: StateFlow<List<FileData>> = combine(_files, _sortOrder) { files, sortOrder ->
        sortFiles(files, sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedDirectories: StateFlow<List<DirectoryData>> = combine(_directories, _sortOrder) { dirs, order ->
        sortDirectories(dirs, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissionsAndLoad()
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedItems.value = current
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
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
        _searchQuery.value = ""
        _isSearching.value = false
        clearSelection()
        sizeCalculationJob?.cancel()
        loadDirectoryContents(directoryUri)
    }

    fun navigateUp() {
        val stack = _directoryStack.value
        if (stack.isNotEmpty()) {
            _currentDirectory.value = stack.last()
            _directoryStack.value = stack.dropLast(1)
            _searchQuery.value = ""
            _isSearching.value = false
            clearSelection()
            sizeCalculationJob?.cancel()
            loadDirectoryContents(stack.last())
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            searchJob?.cancel()
            return
        }
        
        _isSearching.value = true
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) = withContext(Dispatchers.IO) {
        val currentUri = _currentDirectory.value ?: return@withContext
        val results = mutableListOf<SearchResult>()
        
        try {
            if (currentUri.scheme == "file") {
                val rootFile = File(currentUri.path ?: "")
                val stack = mutableListOf(rootFile to 0)
                while (stack.isNotEmpty() && results.size < 100) {
                    val (current, depth) = stack.removeAt(stack.size - 1)
                    val files = try { current.listFiles() } catch (e: Exception) { null } ?: continue
                    for (file in files) {
                        if (file.name.contains(query, ignoreCase = true)) {
                            if (file.isDirectory) {
                                results.add(SearchResult.Directory(createDirectoryData(file)))
                            } else {
                                results.add(SearchResult.File(createFileData(file)))
                            }
                        }
                        if (file.isDirectory && depth < 2) {
                            stack.add(file to depth + 1)
                        }
                        if (results.size >= 100) break
                    }
                }
            } else {
                val context = getApplication<Application>()
                val rootDoc = try { DocumentFile.fromTreeUri(context, currentUri) } catch (e: Exception) { null } ?: return@withContext
                val stack = mutableListOf(rootDoc to 0)
                while (stack.isNotEmpty() && results.size < 100) {
                    val (current, depth) = stack.removeAt(stack.size - 1)
                    val files = try { current.listFiles() } catch (e: Exception) { emptyArray<DocumentFile>() }
                    for (file in files) {
                        if (file.name?.contains(query, ignoreCase = true) == true) {
                            if (file.isDirectory) {
                                results.add(SearchResult.Directory(DirectoryData(
                                    file.uri, 
                                    file.name ?: "", 
                                    0L, 
                                    "", 
                                    file.lastModified(), 
                                    formatDate(file.lastModified()), 
                                    0L, 
                                    file.uri.toString()
                                )))
                            } else {
                                results.add(SearchResult.File(FileData(
                                    file.uri, 
                                    file.name ?: "", 
                                    file.length(), 
                                    Formatter.formatShortFileSize(getApplication(), file.length()), 
                                    file.lastModified(), 
                                    formatDate(file.lastModified()), 
                                    0L, 
                                    file.type, 
                                    file.uri.toString()
                                )))
                            }
                        }
                        if (file.isDirectory && depth < 2) {
                            stack.add(file to depth + 1)
                        }
                        if (results.size >= 100) break
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle permission or other IO errors
        }
        _searchResults.value = results
    }

    fun refresh() {
        val currentUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        sizeCalculationJob?.cancel()
        directoryLoadingJob = viewModelScope.launch {
            _isRefreshing.value = true
            directoryContentCache.remove(currentUri)
            _directories.value.forEach { directorySizeCache.remove(it.uri) }
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

    fun copySelected() {
        val selected = _selectedItems.value.toList()
        if (selected.isNotEmpty()) {
            _clipboardItem.value = ClipboardItem(selected, ClipboardAction.COPY)
            clearSelection()
            showToast("Copied to clipboard")
        }
    }

    fun cutSelected() {
        val selected = _selectedItems.value.toList()
        if (selected.isNotEmpty()) {
            _clipboardItem.value = ClipboardItem(selected, ClipboardAction.CUT)
            clearSelection()
            showToast("Cut to clipboard")
        }
    }

    fun deleteSelected() {
        val selected = _selectedItems.value.toList()
        if (selected.isEmpty()) return
        
        viewModelScope.launch {
            var allSuccess = true
            withContext(Dispatchers.IO) {
                selected.forEach { uri ->
                    try {
                        val deleted = if (uri.scheme == "file") {
                            val file = File(uri.path ?: "")
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        } else {
                            DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, uri)
                        }
                        if (!deleted) allSuccess = false
                    } catch (e: Exception) {
                        allSuccess = false
                    }
                }
            }
            if (allSuccess) {
                showToast("Deleted successfully.")
            } else {
                showToast("Some items could not be deleted.")
            }
            clearSelection()
            invalidateCacheAndReload()
        }
    }

    fun paste() {
        val itemToPaste = _clipboardItem.value ?: return
        val destinationDirUri = _currentDirectory.value ?: return

        viewModelScope.launch {
            val (success, message) = withContext(Dispatchers.IO) {
                try {
                    if (destinationDirUri.scheme == "file") {
                        var allPasted = true
                        itemToPaste.uris.forEach { uri ->
                            if (uri.scheme == "file") {
                                val sourceFile = File(uri.path ?: "")
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
                                if (!result) allPasted = false
                            } else {
                                allPasted = false
                            }
                        }
                        allPasted to if (allPasted) "Pasted successfully." else "Some items failed to paste."
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
                startDirectorySizeCalculations(dirs.filter { it.size == 0L })
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

        val allFiles = try { file.listFiles()?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        val (directoryFiles, regularFiles) = allFiles.partition { it.isDirectory }

        val directoryData = directoryFiles.map { createDirectoryData(it) }
        val fileData = regularFiles.map { createFileData(it) }

        if (directoryUri == _currentDirectory.value) {
            _directories.value = directoryData
            _files.value = fileData
            directoryContentCache[directoryUri] = directoryData to fileData
            startDirectorySizeCalculations(directoryData.filter { it.size == 0L })
        }
    }

    private fun loadFromDocumentFile(directoryUri: Uri) {
        val context = getApplication<Application>()
        val documentFile = try { DocumentFile.fromTreeUri(context, directoryUri) } catch (e: Exception) { null }
        if (documentFile == null || !documentFile.isDirectory) {
            if (directoryUri == _currentDirectory.value) {
                _files.value = emptyList()
                _directories.value = emptyList()
            }
            return
        }

        val allDocs = try { documentFile.listFiles().toList() } catch (e: Exception) { emptyList() }
        val (directoryDocs, fileDocs) = allDocs.partition { it.isDirectory }

        val directoryData = directoryDocs.map { 
            val cachedSize = directorySizeCache[it.uri] ?: 0L
            val formattedSize = if (cachedSize > 0) Formatter.formatShortFileSize(getApplication(), cachedSize) else ""
            DirectoryData(
                it.uri, 
                it.name ?: "", 
                cachedSize, 
                formattedSize, 
                it.lastModified(), 
                formatDate(it.lastModified()), 
                0L, 
                it.uri.toString()
            ) 
        }
        val fileData = mutableListOf<FileData>()

        if (directoryUri == _currentDirectory.value) {
            _directories.value = directoryData
            _files.value = emptyList()
        }

        fileDocs.chunked(30).forEach { chunk ->
            if (directoryUri != _currentDirectory.value) return@forEach
            val fileDataChunk = chunk.map { 
                FileData(
                    it.uri, 
                    it.name ?: "", 
                    it.length(), 
                    Formatter.formatShortFileSize(getApplication(), it.length()), 
                    it.lastModified(), 
                    formatDate(it.lastModified()), 
                    0L, 
                    it.type, 
                    it.uri.toString()
                ) 
            }
            fileData.addAll(fileDataChunk)
            if (directoryUri == _currentDirectory.value) {
                _files.value += fileDataChunk
            }
        }

        if (directoryUri == _currentDirectory.value) {
            directoryContentCache[directoryUri] = directoryData to fileData
            startDirectorySizeCalculations(directoryData.filter { it.size == 0L })
        }
    }

    private fun createFileData(file: File): FileData {
        val creationTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
            } catch (e: Exception) { 0L }
        } else 0L
        return FileData(
            Uri.fromFile(file), 
            file.name, 
            file.length(), 
            Formatter.formatShortFileSize(getApplication(), file.length()), 
            file.lastModified(), 
            formatDate(file.lastModified()), 
            creationTime, 
            getMimeType(file), 
            file.absolutePath
        )
    }

    private fun createDirectoryData(file: File): DirectoryData {
        val cachedSize = directorySizeCache[Uri.fromFile(file)] ?: 0L
        val formattedSize = if (cachedSize > 0) Formatter.formatShortFileSize(getApplication(), cachedSize) else ""
        val creationTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
            } catch (e: Exception) { 0L }
        } else 0L
        return DirectoryData(
            Uri.fromFile(file), 
            file.name, 
            cachedSize, 
            formattedSize, 
            file.lastModified(), 
            formatDate(file.lastModified()), 
            creationTime, 
            file.absolutePath
        )
    }

    private fun startDirectorySizeCalculations(directories: List<DirectoryData>) {
        sizeCalculationJob?.cancel()
        sizeCalculationJob = viewModelScope.launch(Dispatchers.IO) {
            directories.forEach { dir ->
                val size = calculateDirectorySizeIteratively(dir.uri)
                withContext(Dispatchers.Main) {
                    updateDirectorySize(dir.uri, size)
                }
            }
        }
    }

    private fun calculateDirectorySizeIteratively(uri: Uri): Long {
        var totalSize = 0L
        try {
            if (uri.scheme == "file") {
                val root = File(uri.path ?: return 0L)
                val stack = mutableListOf(root)
                while (stack.isNotEmpty()) {
                    val current = stack.removeAt(stack.size - 1)
                    val files = try { current.listFiles() } catch (e: Exception) { null } ?: continue
                    for (file in files) {
                        if (file.isDirectory) {
                            stack.add(file)
                        } else {
                            totalSize += file.length()
                        }
                    }
                }
            } else {
                val context = getApplication<Application>()
                val root = try { DocumentFile.fromTreeUri(context, uri) } catch (e: Exception) { null } ?: return 0L
                val stack = mutableListOf(root)
                while (stack.isNotEmpty()) {
                    val current = stack.removeAt(stack.size - 1)
                    val files = try { current.listFiles() } catch (e: Exception) { emptyArray<DocumentFile>() }
                    for (file in files) {
                        if (file.isDirectory) {
                            stack.add(file)
                        } else {
                            totalSize += file.length()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore permission errors
        }
        return totalSize
    }

    private fun updateDirectorySize(uri: Uri, size: Long) {
        val formattedSize = if (size > 0) Formatter.formatShortFileSize(getApplication(), size) else ""
        directorySizeCache[uri] = size
        _directories.value = _directories.value.map {
            if (it.uri == uri) it.copy(size = size, formattedSize = formattedSize) else it
        }
        _currentDirectory.value?.let { currentUri ->
            directoryContentCache[currentUri]?.let { (dirs, files) ->
                val updatedDirs = dirs.map { if (it.uri == uri) it.copy(size = size, formattedSize = formattedSize) else it }
                directoryContentCache[currentUri] = updatedDirs to files
            }
        }
    }

    private fun getMimeType(file: File): String? =
        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)

    private fun sortFiles(files: List<FileData>, sortOrder: SortOrder): List<FileData> = when (sortOrder) {
        SortOrder.NAME_AZ -> files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOrder.NAME_ZA -> files.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOrder.SIZE_SMALLER -> files.sortedBy { it.size }
        SortOrder.SIZE_LARGER -> files.sortedByDescending { it.size }
        SortOrder.DATE_RECENT -> files.sortedByDescending { it.lastModified }
        SortOrder.DATE_OLDER -> files.sortedBy { it.lastModified }
    }

    private fun sortDirectories(directories: List<DirectoryData>, sortOrder: SortOrder): List<DirectoryData> = 
        when (sortOrder) {
            SortOrder.NAME_AZ -> directories.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOrder.NAME_ZA -> directories.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOrder.SIZE_SMALLER -> directories.sortedBy { it.size }
            SortOrder.SIZE_LARGER -> directories.sortedByDescending { it.size }
            SortOrder.DATE_RECENT -> directories.sortedByDescending { it.lastModified }
            SortOrder.DATE_OLDER -> directories.sortedBy { it.lastModified }
        }

    private fun performFileOperation(operation: suspend (Uri) -> Unit) {
        val directoryUri = _currentDirectory.value ?: return
        directoryLoadingJob?.cancel()
        sizeCalculationJob?.cancel()
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
            directorySizeCache.remove(currentUri)
            loadDirectoryContentsInternal(currentUri)
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch { _toastMessage.emit(message) }
    }
}

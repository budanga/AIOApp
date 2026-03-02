package com.example.aioapp.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.NoteSortOrder
import com.example.aioapp.core.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {
    
    private val _sortOrder = MutableStateFlow(NoteSortOrder.CREATION_DATE)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _rawNotes: StateFlow<List<Note>> = _sortOrder.flatMapLatest { order ->
        repository.getNotes(order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes: StateFlow<List<Note>> = combine(_rawNotes, _searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updatePinnedState(note.id, !note.isPinned)
        }
    }

    fun togglePinForSelected(ids: Set<String>) {
        viewModelScope.launch {
            val selectedNotes = _rawNotes.value.filter { it.id in ids }
            // If all selected are pinned, unpin them. Otherwise, pin them all.
            val allPinned = selectedNotes.all { it.isPinned }
            selectedNotes.forEach { note ->
                repository.updatePinnedState(note.id, !allPinned)
            }
        }
    }

    suspend fun addNote(title: String, content: String, color: Int): Boolean {
        if (!repository.isTitleUnique(title, null)) return false
        
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            content = content,
            color = color,
            isPinned = false,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        
        repository.insertNote(newNote)
        return true
    }

    suspend fun updateNote(id: String, title: String, content: String): Boolean {
        if (!repository.isTitleUnique(title, id)) return false

        val currentNote = _rawNotes.value.find { it.id == id } ?: return false
        val updatedNote = currentNote.copy(
            title = title.trim(),
            content = content,
            modifiedAt = System.currentTimeMillis()
        )
        
        repository.updateNote(updatedNote)
        return true
    }

    fun deleteNotes(ids: Set<String>) {
        viewModelScope.launch {
            repository.deleteNotes(ids.toList())
        }
    }

    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }
}

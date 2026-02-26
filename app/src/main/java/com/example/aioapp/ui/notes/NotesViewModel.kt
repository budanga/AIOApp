package com.example.aioapp.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.NoteSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

class NotesViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    
    private val _sortOrder = MutableStateFlow(NoteSortOrder.CREATION_DATE)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(_notes, _sortOrder) { notes, order ->
        when (order) {
            NoteSortOrder.ALPHABETICAL -> notes.sortedBy { it.title }
            NoteSortOrder.CREATION_DATE -> notes.sortedByDescending { it.createdAt }
            NoteSortOrder.MODIFICATION_DATE -> notes.sortedByDescending { it.modifiedAt }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun isTitleUnique(title: String, excludeId: String? = null): Boolean {
        return _notes.value.none { 
            it.title.trim().equals(title.trim(), ignoreCase = true) && it.id != excludeId 
        }
    }

    fun addNote(title: String, content: String, color: Int): Boolean {
        if (!isTitleUnique(title)) return false
        
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            content = content,
            color = color,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        _notes.update { it + newNote }
        return true
    }

    fun updateNote(id: String, title: String, content: String): Boolean {
        if (!isTitleUnique(title, id)) return false

        _notes.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        title = title.trim(),
                        content = content,
                        modifiedAt = System.currentTimeMillis()
                    )
                } else it
            }
        }
        return true
    }

    fun deleteNotes(ids: Set<String>) {
        _notes.update { list ->
            list.filterNot { it.id in ids }
        }
    }

    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }
}

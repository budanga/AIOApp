package com.example.aioapp.core.repository

import com.example.aioapp.core.database.NoteDao
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.NoteSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getNotes(sortOrder: NoteSortOrder): Flow<List<Note>> {
        return when (sortOrder) {
            NoteSortOrder.ALPHABETICAL -> noteDao.getAllNotesAlphabetical()
            NoteSortOrder.CREATION_DATE -> noteDao.getAllNotesByCreationDate()
            NoteSortOrder.MODIFICATION_DATE -> noteDao.getAllNotesByModificationDate()
        }
    }

    suspend fun isTitleUnique(title: String, excludeId: String?): Boolean = withContext(Dispatchers.IO) {
        !noteDao.existsByTitle(title.trim(), excludeId ?: "")
    }

    suspend fun insertNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNotes(ids: List<String>) = withContext(Dispatchers.IO) {
        noteDao.deleteNotes(ids)
    }
}

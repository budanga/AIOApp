package com.example.aioapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.aioapp.core.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, title ASC")
    fun getAllNotesAlphabetical(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotesByCreationDate(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, modifiedAt DESC")
    fun getAllNotesByModificationDate(): Flow<List<Note>>

    @Query("SELECT EXISTS(SELECT 1 FROM notes WHERE title = :title AND id != :excludeId)")
    suspend fun existsByTitle(title: String, excludeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedState(id: String, isPinned: Boolean)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotes(ids: List<String>)
}

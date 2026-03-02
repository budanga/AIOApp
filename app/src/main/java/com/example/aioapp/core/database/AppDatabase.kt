package com.example.aioapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aioapp.core.model.Note

@Database(entities = [Note::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

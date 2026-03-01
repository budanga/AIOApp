package com.example.aioapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.CurrencyRate
import com.example.aioapp.core.model.UnitOrder

@Database(entities = [Note::class, CurrencyRate::class, UnitOrder::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun unitOrderDao(): UnitOrderDao
}

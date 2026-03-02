package com.example.aioapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aioapp.core.model.Note
import com.example.aioapp.core.model.CurrencyRate
import com.example.aioapp.core.model.UnitOrder
import com.example.aioapp.core.model.TrucoGame

@Database(
    entities = [
        Note::class,
        TrucoGame::class,
        CurrencyRate::class,
        UnitOrder::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun unitOrderDao(): UnitOrderDao
    abstract fun trucoDao(): TrucoDao
}

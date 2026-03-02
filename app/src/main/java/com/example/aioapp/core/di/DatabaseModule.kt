package com.example.aioapp.core.di

import android.content.Context
import androidx.room.Room
import com.example.aioapp.core.database.AppDatabase
import com.example.aioapp.core.database.NoteDao
import com.example.aioapp.core.database.CurrencyDao
import com.example.aioapp.core.database.UnitOrderDao
import com.example.aioapp.core.database.TrucoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aio_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideCurrencyDao(database: AppDatabase): CurrencyDao {
        return database.currencyDao()
    }

    @Provides
    fun provideUnitOrderDao(database: AppDatabase): UnitOrderDao {
        return database.unitOrderDao()
    }

    @Provides
    fun provideTrucoDao(database: AppDatabase): TrucoDao {
        return database.trucoDao()
    }
}

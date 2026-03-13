package com.example.aioapp.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRate(
    @PrimaryKey val code: String,
    val rate: Double,
    val lastUpdated: Long
)

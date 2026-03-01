package com.example.aioapp.core.model

import androidx.room.Entity

@Entity(tableName = "unit_orders", primaryKeys = ["category", "unitName"])
data class UnitOrder(
    val category: String,
    val unitName: String,
    val displayOrder: Int
)

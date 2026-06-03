package com.example.aioapp.ui.bbq

data class BBQUiState(
    val menCount: Int = 0,
    val womenCount: Int = 0,
    val childrenCount: Int = 0,
    
    val enableExpenseSplitter: Boolean = false,
    val totalCostInput: String = "",
    
    val totalMeatKg: Double = 0.0,
    val totalBreadUnits: Int = 0,
    val totalCoalKg: Double = 0.0,
    val totalDrinksLiters: Double = 0.0,
    val costPerAdult: Double? = null
)

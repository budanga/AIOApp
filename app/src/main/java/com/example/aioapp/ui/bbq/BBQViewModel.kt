package com.example.aioapp.ui.bbq

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BBQViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(BBQUiState())
    val uiState: StateFlow<BBQUiState> = _uiState.asStateFlow()

    fun updateMenCount(count: Int) {
        if(count >= 0) {
            _uiState.update { it.copy(menCount = count) }
            calculate()
        }
    }
    
    fun updateWomenCount(count: Int) {
        if(count >= 0) {
            _uiState.update { it.copy(womenCount = count) }
            calculate()
        }
    }
    
    fun updateChildrenCount(count: Int) {
        if(count >= 0) {
            _uiState.update { it.copy(childrenCount = count) }
            calculate()
        }
    }

    fun toggleExpenseSplitter(enable: Boolean) {
        _uiState.update { it.copy(enableExpenseSplitter = enable) }
        calculate()
    }
    
    fun updateTotalCost(costStr: String) {
        _uiState.update { it.copy(totalCostInput = costStr) }
        calculate()
    }
    
    private fun calculate() {
        val state = _uiState.value
        val totalAdults = state.menCount + state.womenCount
        val totalPeople = totalAdults + state.childrenCount
        
        val meatKg = (state.menCount * 0.500) + (state.womenCount * 0.350) + (state.childrenCount * 0.250)
        
        val breadUnits = (state.menCount * 2) + (state.womenCount * 1) + (state.childrenCount * 1)
        
        val coalKg = meatKg * 1.0 // 1kg per kg of meat
        
        val drinksLiters = totalPeople * 1.0
        
        val costPerAdult = if (state.enableExpenseSplitter) {
            val costStr = state.totalCostInput.replace(',', '.')
            val cost = costStr.toDoubleOrNull()
            if (cost != null && totalAdults > 0) {
                cost / totalAdults
            } else null
        } else null
        
        _uiState.update { 
            it.copy(
                totalMeatKg = meatKg,
                totalBreadUnits = breadUnits,
                totalCoalKg = coalKg,
                totalDrinksLiters = drinksLiters,
                costPerAdult = costPerAdult
            )
        }
    }
}

package com.example.aioapp.ui.paymentcomparator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PaymentComparatorUiState(
    val priceInput: String = "",
    val installmentsInput: Int = 12,
    val availableInstallments: List<Int> = listOf(1, 3, 6, 9, 12, 18),
    val tnaInput: String = "",
    val inflationInput: String = "",
    val result: PaymentComparisonResult? = null,
    val priceError: Boolean = false,
    val tnaError: Boolean = false,
    val showBreakdown: Boolean = false
)

@HiltViewModel
class PaymentComparatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentComparatorUiState())
    val uiState: StateFlow<PaymentComparatorUiState> = _uiState.asStateFlow()

    fun onPriceChange(value: String) {
        _uiState.update { it.copy(priceInput = value, priceError = false) }
        recalculate()
    }

    private val initialInstallments = listOf(1, 3, 6, 9, 12, 18)

    fun onInstallmentsChange(value: Int) {
        _uiState.update { it.copy(installmentsInput = value) }
        recalculate()
    }

    fun addCustomInstallment(value: Int) {
        _uiState.update { state ->
            val newList = if (initialInstallments.contains(value)) {
                initialInstallments
            } else {
                initialInstallments + value
            }
            state.copy(availableInstallments = newList, installmentsInput = value)
        }
        recalculate()
    }

    fun onTnaChange(value: String) {
        _uiState.update { it.copy(tnaInput = value, tnaError = false) }
        recalculate()
    }

    fun onInflationChange(value: String) {
        _uiState.update { it.copy(inflationInput = value) }
        recalculate()
    }

    fun toggleBreakdown() {
        _uiState.update { it.copy(showBreakdown = !it.showBreakdown) }
    }

    private fun recalculate() {
        val state = _uiState.value
        val price = state.priceInput.toDoubleOrNull()
        val installments = state.installmentsInput
        val tna = state.tnaInput.toDoubleOrNull()?.div(100.0)
        val inflation = state.inflationInput.toDoubleOrNull()?.div(100.0)

        val priceError = state.priceInput.isNotBlank() && (price == null || price <= 0)
        val tnaError = state.tnaInput.isNotBlank() && (tna == null || tna < 0)

        if (priceError || tnaError) {
            _uiState.update { it.copy(priceError = priceError, tnaError = tnaError, result = null) }
            return
        }

        val result = if (price != null && price > 0 && installments > 0 && tna != null && tna >= 0) {
            PaymentComparatorCalculator.compare(
                price = price,
                installments = installments,
                tna = tna,
                monthlyInflation = inflation
            )
        } else {
            null
        }

        _uiState.update { it.copy(result = result, priceError = false, tnaError = false) }
    }
}

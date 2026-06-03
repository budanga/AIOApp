package com.example.aioapp.ui.paymentcomparator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.datastore.PaymentComparatorPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentComparatorUiState(
    val priceInput: String = "",
    val installmentsInput: Int = 12,
    val tnaInput: String = "",
    val inflationInput: String = "",
    val result: PaymentComparisonResult? = null,
    val priceError: Boolean = false,
    val tnaError: Boolean = false,
    val showBreakdown: Boolean = false
)

@HiltViewModel
class PaymentComparatorViewModel @Inject constructor(
    private val repository: PaymentComparatorPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentComparatorUiState())
    val uiState: StateFlow<PaymentComparatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = repository.paymentPrefsFlow.first()
            _uiState.update {
                it.copy(
                    priceInput = prefs.price,
                    installmentsInput = prefs.installments,
                    tnaInput = prefs.tna,
                    inflationInput = prefs.inflation
                )
            }
            recalculate()
        }
    }

    fun onPriceChange(value: String) {
        _uiState.update { it.copy(priceInput = value, priceError = false) }
        viewModelScope.launch { repository.savePrice(value) }
        recalculate()
    }

    fun onInstallmentsChange(value: Int) {
        _uiState.update { it.copy(installmentsInput = value) }
        viewModelScope.launch { repository.saveInstallments(value) }
        recalculate()
    }

    fun onTnaChange(value: String) {
        _uiState.update { it.copy(tnaInput = value, tnaError = false) }
        viewModelScope.launch { repository.saveTna(value) }
        recalculate()
    }

    fun onInflationChange(value: String) {
        _uiState.update { it.copy(inflationInput = value) }
        viewModelScope.launch { repository.saveInflation(value) }
        recalculate()
    }

    fun toggleBreakdown() {
        _uiState.update { it.copy(showBreakdown = !it.showBreakdown) }
    }

    private fun recalculate() {
        val state = _uiState.value
        val price = state.priceInput.toDoubleOrNull()
        val tna = state.tnaInput.toDoubleOrNull()?.div(100.0)
        val inflation = state.inflationInput.toDoubleOrNull()?.div(100.0)

        val priceError = state.priceInput.isNotBlank() && (price == null || price <= 0)
        val tnaError = state.tnaInput.isNotBlank() && (tna == null || tna < 0)

        if (priceError || tnaError) {
            _uiState.update { it.copy(priceError = priceError, tnaError = tnaError, result = null) }
            return
        }

        val result = if (price != null && price > 0 && tna != null && tna >= 0) {
            PaymentComparatorCalculator.compare(
                price = price,
                installments = state.installmentsInput,
                tna = tna,
                monthlyInflation = inflation
            )
        } else {
            null
        }

        _uiState.update { it.copy(result = result, priceError = false, tnaError = false) }
    }
}

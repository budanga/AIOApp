package com.example.aioapp.ui.paymentcomparator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.datastore.PaymentComparatorPreferencesRepository
import com.example.aioapp.core.repository.InflationRepository
import com.example.aioapp.core.repository.MonthlyInflation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Where the inflation value in the form comes from. */
sealed interface InflationStatus {
    data object Loading : InflationStatus
    /** Filled from the official series; editing the field switches to [Manual]. */
    data class Fetched(val month: YearMonth) : InflationStatus
    /** The request failed (no connection, server error): the user types it in. */
    data object Failed : InflationStatus
    data object Manual : InflationStatus
}

/** Preset installment counts shown as chips; anything else goes through the custom chip. */
val installmentOptions = listOf(1, 2, 3, 6, 9, 12, 18, 24)

/** Upper bound for custom installments: keeps the monthly breakdown list a sane size. */
const val MAX_INSTALLMENTS = 360

data class PaymentComparatorUiState(
    val priceInput: String = "",
    val installmentsInput: Int = 12,
    val customInstallmentsSelected: Boolean = false,
    val customInstallmentsInput: String = "",
    val installmentsError: Boolean = false,
    val tnaInput: String = "",
    val inflationInput: String = "",
    val result: PaymentComparisonResult? = null,
    val priceError: Boolean = false,
    val tnaError: Boolean = false,
    val showBreakdown: Boolean = false,
    val showInputs: Boolean = true,
    /** False until saved inputs are read; the screen waits so it opens in its final layout, not animating into it. */
    val loaded: Boolean = false,
    val inflationStatus: InflationStatus = InflationStatus.Loading
)

private const val SAVE_DEBOUNCE_MS = 300L

@HiltViewModel
class PaymentComparatorViewModel @Inject constructor(
    private val repository: PaymentComparatorPreferencesRepository,
    private val inflationRepository: InflationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentComparatorUiState())
    val uiState: StateFlow<PaymentComparatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = repository.paymentPrefsFlow.first()
            val isCustom = prefs.installments !in installmentOptions
            _uiState.update {
                it.copy(
                    priceInput = prefs.price,
                    installmentsInput = prefs.installments,
                    customInstallmentsSelected = isCustom,
                    customInstallmentsInput = if (isCustom) prefs.installments.toString() else "",
                    tnaInput = prefs.tna,
                    inflationInput = prefs.inflation
                )
            }
            recalculate()
            // Returning users with a complete calculation land on the verdict, not the form
            _uiState.update { it.copy(showInputs = it.result == null, loaded = true) }
            fetchInflation()
        }
    }

    fun fetchInflation() {
        viewModelScope.launch {
            _uiState.update { it.copy(inflationStatus = InflationStatus.Loading) }
            inflationRepository.latestMonthlyInflation()
                .onSuccess(::applyFetchedInflation)
                // Keep the value saved last time so the user only has to fix it, not retype it
                .onFailure { _uiState.update { it.copy(inflationStatus = InflationStatus.Failed) } }
        }
    }

    private fun applyFetchedInflation(inflation: MonthlyInflation) {
        // The user started typing while the request was in flight: their value wins
        if (_uiState.value.inflationStatus == InflationStatus.Manual) return
        _uiState.update {
            it.copy(
                inflationInput = inflation.percent.toString(),
                inflationStatus = InflationStatus.Fetched(inflation.month)
            )
        }
        persist()
        recalculate()
    }

    fun onPriceChange(value: String) {
        _uiState.update { it.copy(priceInput = value, priceError = false) }
        persist()
        recalculate()
    }

    fun onInstallmentsChange(value: Int) {
        _uiState.update { it.copy(installmentsInput = value, customInstallmentsSelected = false) }
        persist()
        recalculate()
    }

    fun onCustomInstallmentsSelected() {
        _uiState.update { it.copy(customInstallmentsSelected = true) }
        persist()
        recalculate()
    }

    fun onCustomInstallmentsChange(value: String) {
        // Whole installments only; the decimal keyboard can't be fully trusted to filter
        _uiState.update { it.copy(customInstallmentsInput = value.filter(Char::isDigit)) }
        persist()
        recalculate()
    }

    /** Installment count currently in effect, or null while the custom field is blank or invalid. */
    private fun PaymentComparatorUiState.effectiveInstallments(): Int? =
        if (customInstallmentsSelected) {
            customInstallmentsInput.toIntOrNull()?.takeIf { it in 1..MAX_INSTALLMENTS }
        } else {
            installmentsInput
        }

    fun onTnaChange(value: String) {
        _uiState.update { it.copy(tnaInput = value, tnaError = false) }
        persist()
        recalculate()
    }

    fun onInflationChange(value: String) {
        _uiState.update { it.copy(inflationInput = value, inflationStatus = InflationStatus.Manual) }
        persist()
        recalculate()
    }

    private var saveJob: Job? = null

    /** Writes all inputs once typing pauses, instead of one DataStore write per keystroke. */
    private fun persist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        val state = _uiState.value
        // An invalid custom value isn't saved; the last chip value stays as the fallback
        val installments = state.effectiveInstallments() ?: state.installmentsInput
        repository.saveAll(state.priceInput, installments, state.tnaInput, state.inflationInput)
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen inside the debounce window must not drop the last edit
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            CoroutineScope(Dispatchers.IO).launch { saveNow() }
        }
    }

    fun toggleInputs() {
        _uiState.update { it.copy(showInputs = !it.showInputs) }
    }

    fun toggleBreakdown() {
        _uiState.update { it.copy(showBreakdown = !it.showBreakdown) }
    }

    private fun recalculate() {
        val state = _uiState.value
        val price = state.priceInput.toDoubleOrNull()
        val tna = state.tnaInput.toDoubleOrNull()?.div(100.0)
        val inflation = state.inflationInput.toDoubleOrNull()?.div(100.0)

        val installments = state.effectiveInstallments()

        val priceError = state.priceInput.isNotBlank() && (price == null || price <= 0)
        val tnaError = state.tnaInput.isNotBlank() && (tna == null || tna < 0)
        val installmentsError = state.customInstallmentsSelected &&
            state.customInstallmentsInput.isNotBlank() && installments == null

        if (priceError || tnaError || installmentsError) {
            // Errors live in the form, so it must be visible to show them
            _uiState.update {
                it.copy(
                    priceError = priceError,
                    tnaError = tnaError,
                    installmentsError = installmentsError,
                    result = null,
                    showInputs = true
                )
            }
            return
        }

        val result = if (price != null && price > 0 && tna != null && tna >= 0 && installments != null) {
            PaymentComparatorCalculator.compare(
                price = price,
                installments = installments,
                tna = tna,
                monthlyInflation = inflation
            )
        } else {
            null
        }

        _uiState.update { it.copy(result = result, priceError = false, tnaError = false, installmentsError = false) }
    }
}

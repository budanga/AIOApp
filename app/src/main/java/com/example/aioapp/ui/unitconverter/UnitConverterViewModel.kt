package com.example.aioapp.ui.unitconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aioapp.core.model.UnitOrder
import com.example.aioapp.core.repository.CurrencyRepository
import com.example.aioapp.core.repository.UnitOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UnitCategory {
    MASS, LENGTH, TEMPERATURE, SPEED, VOLUME, TIME, STORAGE, ENERGY, PRESSURE, ELECTRICAL, CURRENCY
}

data class ConversionResult(
    val unitId: String,
    val unitName: String,
    val value: String,
    val symbol: String,
    val rate: Double? = null
)

data class UnitConverterUiState(
    val selectedCategory: UnitCategory = UnitCategory.MASS,
    val inputValue: String = "",
    /** Stores the stable [UnitInfo.id], not the display name. */
    val fromUnit: String = "",
    val availableUnits: List<UnitInfo> = emptyList(),
    val results: List<ConversionResult> = emptyList(),
    val lastUpdated: Long? = null,
    val errorMessage: String? = null
)

data class UnitInfo(
    /** Stable, locale-independent key used for persistence and conversion logic. */
    val id: String,
    /** Display-only — do not use as a logic or persistence key. */
    val name: String,
    val symbol: String,
    val factor: Double,
    val rate: Double? = null,
    val displayOrder: Int = 0
)

@OptIn(FlowPreview::class)
@HiltViewModel
class UnitConverterViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val unitOrderRepository: UnitOrderRepository
) : ViewModel() {

    private val supportedCurrencyCodes = listOf("USD", "EUR", "ARS", "CLP", "UYU", "BRL", "JPY", "CNY")

    private val commonCurrencies = listOf(
        UnitInfo("USD", "US Dollar", "USD", 1.0, 1.0),
        UnitInfo("EUR", "Euro", "EUR", 1.0, 0.92),
        UnitInfo("ARS", "Argentine Peso", "ARS", 1.0, 840.0),
        UnitInfo("CLP", "Chilean Peso", "CLP", 1.0, 970.0),
        UnitInfo("UYU", "Uruguayan Peso", "UYU", 1.0, 39.0),
        UnitInfo("BRL", "Brazilian Real", "BRL", 1.0, 4.97),
        UnitInfo("JPY", "Japanese Yen", "JPY", 1.0, 150.0),
        UnitInfo("CNY", "Chinese Yuan", "CNY", 1.0, 7.19)
    )

    private fun getCurrencyInfo(code: String): Pair<String, String> = when (code) {
        "USD" -> "US Dollar" to "USD"
        "EUR" -> "Euro" to "EUR"
        "ARS" -> "Argentine Peso" to "ARS"
        "CLP" -> "Chilean Peso" to "CLP"
        "UYU" -> "Uruguayan Peso" to "UYU"
        "BRL" -> "Brazilian Real" to "BRL"
        "JPY" -> "Japanese Yen" to "JPY"
        "CNY" -> "Chinese Yuan" to "CNY"
        else -> code to code
    }

    private val _uiState = MutableStateFlow(UnitConverterUiState())
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    private val _inputFlow = MutableSharedFlow<String>()

    private val unitsMap = mapOf(
        UnitCategory.MASS to listOf(
            UnitInfo("kilogram", "Kilogram", "kg", 1.0),
            UnitInfo("gram", "Gram", "g", 1000.0),
            UnitInfo("milligram", "Milligram", "mg", 1000000.0),
            UnitInfo("pound", "Pound", "lb", 2.20462),
            UnitInfo("ounce", "Ounce", "oz", 35.274)
        ),
        UnitCategory.LENGTH to listOf(
            UnitInfo("meter", "Meter", "m", 1.0),
            UnitInfo("kilometer", "Kilometer", "km", 0.001),
            UnitInfo("centimeter", "Centimeter", "cm", 100.0),
            UnitInfo("millimeter", "Millimeter", "mm", 1000.0),
            UnitInfo("mile", "Mile", "mi", 0.000621371),
            UnitInfo("yard", "Yard", "yd", 1.09361),
            UnitInfo("foot", "Foot", "ft", 3.28084),
            UnitInfo("inch", "Inch", "in", 39.3701)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitInfo("celsius", "Celsius", "°C", 1.0),
            UnitInfo("fahrenheit", "Fahrenheit", "°F", 1.0),
            UnitInfo("kelvin", "Kelvin", "K", 1.0)
        ),
        UnitCategory.SPEED to listOf(
            UnitInfo("meter_per_second", "Meter/second", "m/s", 1.0),
            UnitInfo("kilometer_per_hour", "Kilometer/hour", "km/h", 3.6),
            UnitInfo("mile_per_hour", "Mile/hour", "mph", 2.23694),
            UnitInfo("knot", "Knot", "kn", 1.94384),
            UnitInfo("foot_per_second", "Foot/second", "ft/s", 3.28084)
        ),
        UnitCategory.VOLUME to listOf(
            UnitInfo("liter", "Liter", "L", 1.0),
            UnitInfo("milliliter", "Milliliter", "mL", 1000.0),
            UnitInfo("cubic_meter", "Cubic meter", "m³", 0.001),
            UnitInfo("gallon", "Gallon", "gal", 0.264172),
            UnitInfo("quart", "Quart", "qt", 1.05669),
            UnitInfo("pint", "Pint", "pt", 2.11338),
            UnitInfo("cup", "Cup", "cup", 4.22675)
        ),
        UnitCategory.TIME to listOf(
            UnitInfo("second", "Second", "s", 1.0),
            UnitInfo("millisecond", "Millisecond", "ms", 1000.0),
            UnitInfo("minute", "Minute", "min", 1.0 / 60.0),
            UnitInfo("hour", "Hour", "h", 1.0 / 3600.0),
            UnitInfo("day", "Day", "d", 1.0 / 86400.0),
            UnitInfo("week", "Week", "wk", 1.0 / 604800.0)
        ),
        UnitCategory.STORAGE to listOf(
            UnitInfo("byte", "Byte", "B", 1.0),
            UnitInfo("kilobyte", "Kilobyte", "KB", 1.0 / 1024.0),
            UnitInfo("megabyte", "Megabyte", "MB", 1.0 / (1024.0 * 1024.0)),
            UnitInfo("gigabyte", "Gigabyte", "GB", 1.0 / (1024.0 * 1024.0 * 1024.0)),
            UnitInfo("terabyte", "Terabyte", "TB", 1.0 / (1024.0 * 1024.0 * 1024.0 * 1024.0))
        ),
        UnitCategory.ENERGY to listOf(
            UnitInfo("joule", "Joule", "J", 1.0),
            UnitInfo("kilojoule", "Kilojoule", "kJ", 0.001),
            UnitInfo("calorie", "Calorie", "cal", 0.239006),
            UnitInfo("kilocalorie", "Kilocalorie", "kcal", 0.000239006),
            UnitInfo("watt_hour", "Watt-hour", "Wh", 0.000277778),
            UnitInfo("kilowatt_hour", "Kilowatt-hour", "kWh", 2.77778e-7)
        ),
        UnitCategory.PRESSURE to listOf(
            UnitInfo("pascal", "Pascal", "Pa", 1.0),
            UnitInfo("kilopascal", "Kilopascal", "kPa", 0.001),
            UnitInfo("bar", "Bar", "bar", 0.00001),
            UnitInfo("psi", "PSI", "psi", 0.000145038),
            UnitInfo("atmosphere", "Atmosphere", "atm", 9.86923e-6)
        ),
        UnitCategory.ELECTRICAL to listOf(
            UnitInfo("ampere", "Ampere", "A", 1.0),
            UnitInfo("milliampere", "Milliampere", "mA", 1000.0),
            UnitInfo("microampere", "Microampere", "µA", 1000000.0),
            UnitInfo("volt", "Volt", "V", 1.0),
            UnitInfo("ohm", "Ohm", "Ω", 1.0)
        )
    )

    init {
        _inputFlow
            .debounce(150)
            .onEach { performConversion() }
            .launchIn(viewModelScope)

        combine(
            currencyRepository.getRates(),
            _uiState.map { it.selectedCategory }.distinctUntilChanged(),
            ::Pair
        ).onEach { (rates, category) ->
            if (category == UnitCategory.CURRENCY) {
                val filteredRates = rates.filter { it.code in supportedCurrencyCodes }
                val baseUnitInfos = if (filteredRates.isEmpty()) commonCurrencies else filteredRates.map { rate ->
                    val (name, symbol) = getCurrencyInfo(rate.code)
                    UnitInfo(rate.code, name, symbol, 1.0, rate.rate)
                }
                loadAndApplyOrders(category, baseUnitInfos)
            }
        }.launchIn(viewModelScope)

        _uiState.map { it.selectedCategory }
            .distinctUntilChanged()
            .onEach { category ->
                if (category != UnitCategory.CURRENCY) {
                    val baseUnits = unitsMap[category] ?: emptyList()
                    loadAndApplyOrders(category, baseUnits)
                }
            }.launchIn(viewModelScope)

        selectCategory(UnitCategory.MASS)
    }

    private fun loadAndApplyOrders(category: UnitCategory, baseUnits: List<UnitInfo>) {
        viewModelScope.launch {
            unitOrderRepository.getOrdersByCategory(category.name).first().let { savedOrders ->
                val orderedUnits = if (savedOrders.isEmpty()) {
                    baseUnits.mapIndexed { index, unitInfo -> unitInfo.copy(displayOrder = index) }
                } else {
                    baseUnits.map { unit ->
                        val order = savedOrders.find { it.unitName == unit.id }?.displayOrder ?: 999
                        unit.copy(displayOrder = order)
                    }.sortedBy { it.displayOrder }
                }

                _uiState.update { state ->
                    val currentFromUnit = state.fromUnit
                    val newFromUnit = if (currentFromUnit.isEmpty() || orderedUnits.none { it.id == currentFromUnit }) {
                        if (category == UnitCategory.CURRENCY) {
                            orderedUnits.find { it.id == "USD" }?.id ?: orderedUnits.firstOrNull()?.id ?: ""
                        } else {
                            orderedUnits.firstOrNull()?.id ?: ""
                        }
                    } else {
                        currentFromUnit
                    }
                    state.copy(
                        availableUnits = orderedUnits,
                        fromUnit = newFromUnit
                    )
                }
                performConversion()
            }
        }
    }

    fun selectCategory(category: UnitCategory) {
        if (category == UnitCategory.CURRENCY) {
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    inputValue = "",
                    results = emptyList(),
                    lastUpdated = System.currentTimeMillis(),
                    errorMessage = null
                )
            }
            fetchCurrencyRates()
        } else {
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    inputValue = "",
                    results = emptyList(),
                    lastUpdated = null,
                    errorMessage = null
                )
            }
        }
    }

    private fun fetchCurrencyRates() {
        viewModelScope.launch {
            val result = currencyRepository.refreshRates()
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Failed to fetch latest rates. Using cached/default data.") }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputValue = value) }
        viewModelScope.launch { _inputFlow.emit(value) }
    }

    fun onFromUnitChange(unitId: String) {
        _uiState.update { it.copy(fromUnit = unitId) }
        performConversion()
    }

    fun moveUnit(fromIndex: Int, toIndex: Int) {
        val currentUnits = _uiState.value.availableUnits.toMutableList()
        if (fromIndex !in currentUnits.indices || toIndex !in currentUnits.indices) return

        val item = currentUnits.removeAt(fromIndex)
        currentUnits.add(toIndex, item)

        val updatedUnits = currentUnits.mapIndexed { index, unitInfo ->
            unitInfo.copy(displayOrder = index)
        }

        val currentResults = _uiState.value.results.toMutableList()
        if (fromIndex in currentResults.indices && toIndex in currentResults.indices) {
            val resItem = currentResults.removeAt(fromIndex)
            currentResults.add(toIndex, resItem)
        }

        _uiState.update {
            it.copy(
                availableUnits = updatedUnits,
                results = currentResults
            )
        }
    }

    fun saveReorderedUnits() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val orders = currentState.availableUnits.map {
                UnitOrder(currentState.selectedCategory.name, it.id, it.displayOrder)
            }
            unitOrderRepository.saveOrders(orders)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun performConversion() {
        val currentState = _uiState.value
        val input = currentState.inputValue.toDoubleOrNull() ?: 0.0
        val category = currentState.selectedCategory
        val units = currentState.availableUnits

        if (units.isEmpty() && category != UnitCategory.CURRENCY) return

        val fromUnit = units.find { it.id == currentState.fromUnit } ?: units.firstOrNull() ?: return

        val baseValue = convertToBase(input, fromUnit, category)

        val results = units.map { toUnit ->
            val convertedValue = convertFromBase(baseValue, toUnit, category)
            ConversionResult(
                unitId = toUnit.id,
                unitName = toUnit.name,
                value = formatValue(convertedValue),
                symbol = toUnit.symbol,
                rate = if (category == UnitCategory.CURRENCY) toUnit.rate else null
            )
        }

        _uiState.update { it.copy(results = results) }
    }

    private fun convertToBase(value: Double, unit: UnitInfo, category: UnitCategory): Double {
        return when (category) {
            UnitCategory.TEMPERATURE -> when (unit.id) {
                "celsius" -> value
                "fahrenheit" -> (value - 32) * 5 / 9
                "kelvin" -> value - 273.15
                else -> value
            }
            UnitCategory.CURRENCY -> {
                val rate = unit.rate ?: 1.0
                if (rate == 0.0) 0.0 else value / rate
            }
            else -> value / unit.factor
        }
    }

    private fun convertFromBase(baseValue: Double, unit: UnitInfo, category: UnitCategory): Double {
        return when (category) {
            UnitCategory.TEMPERATURE -> when (unit.id) {
                "celsius" -> baseValue
                "fahrenheit" -> (baseValue * 9 / 5) + 32
                "kelvin" -> baseValue + 273.15
                else -> baseValue
            }
            UnitCategory.CURRENCY -> baseValue * (unit.rate ?: 1.0)
            else -> baseValue * unit.factor
        }
    }

    private fun formatValue(value: Double): String {
        if (_uiState.value.inputValue.isEmpty()) return "0"
        return if (value % 1.0 == 0.0 && value < 1e12) {
            value.toLong().toString()
        } else {
            "%.4f".format(value).trimEnd('0').trimEnd('.')
        }
    }
}

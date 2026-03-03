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
    MASS, LENGTH, TEMPERATURE, SPEED, VOLUME, TIME, STORAGE, ENERGY, PRESSURE, ELECTRICAL, CURRENCY, NUMBERS
}

data class ConversionResult(
    val unitName: String,
    val value: String,
    val symbol: String,
    val rate: Double? = null
)

data class UnitConverterUiState(
    val selectedCategory: UnitCategory = UnitCategory.MASS,
    val inputValue: String = "",
    val fromUnit: String = "",
    val availableUnits: List<UnitInfo> = emptyList(),
    val results: List<ConversionResult> = emptyList(),
    val lastUpdated: Long? = null,
    val errorMessage: String? = null,
    val customBase: String = "32"
)

data class UnitInfo(
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
        UnitInfo("US Dollar", "USD", 1.0, 1.0),
        UnitInfo("Euro", "EUR", 1.0, 0.92),
        UnitInfo("Argentine Peso", "ARS", 1.0, 840.0),
        UnitInfo("Chilean Peso", "CLP", 1.0, 970.0),
        UnitInfo("Uruguayan Peso", "UYU", 1.0, 39.0),
        UnitInfo("Brazilian Real", "BRL", 1.0, 4.97),
        UnitInfo("Japanese Yen", "JPY", 1.0, 150.0),
        UnitInfo("Chinese Yuan", "CNY", 1.0, 7.19)
    )

    private fun getCurrencyName(code: String): String {
        return when (code) {
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "ARS" -> "Argentine Peso"
            "CLP" -> "Chilean Peso"
            "UYU" -> "Uruguayan Peso"
            "BRL" -> "Brazilian Real"
            "JPY" -> "Japanese Yen"
            "CNY" -> "Chinese Yuan"
            else -> code
        }
    }

    private val _uiState = MutableStateFlow(UnitConverterUiState())
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    private val _inputFlow = MutableSharedFlow<String>()

    private val unitsMap = mapOf(
        UnitCategory.MASS to listOf(
            UnitInfo("Kilogram", "kg", 1.0),
            UnitInfo("Gram", "g", 1000.0),
            UnitInfo("Milligram", "mg", 1000000.0),
            UnitInfo("Pound", "lb", 2.20462),
            UnitInfo("Ounce", "oz", 35.274)
        ),
        UnitCategory.LENGTH to listOf(
            UnitInfo("Meter", "m", 1.0),
            UnitInfo("Kilometer", "km", 0.001),
            UnitInfo("Centimeter", "cm", 100.0),
            UnitInfo("Millimeter", "mm", 1000.0),
            UnitInfo("Mile", "mi", 0.000621371),
            UnitInfo("Yard", "yd", 1.09361),
            UnitInfo("Foot", "ft", 3.28084),
            UnitInfo("Inch", "in", 39.3701)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitInfo("Celsius", "°C", 1.0),
            UnitInfo("Fahrenheit", "°F", 1.0),
            UnitInfo("Kelvin", "K", 1.0)
        ),
        UnitCategory.SPEED to listOf(
            UnitInfo("Meter/second", "m/s", 1.0),
            UnitInfo("Kilometer/hour", "km/h", 3.6),
            UnitInfo("Mile/hour", "mph", 2.23694),
            UnitInfo("Knot", "kn", 1.94384),
            UnitInfo("Foot/second", "ft/s", 3.28084)
        ),
        UnitCategory.VOLUME to listOf(
            UnitInfo("Liter", "L", 1.0),
            UnitInfo("Milliliter", "mL", 1000.0),
            UnitInfo("Cubic meter", "m³", 0.001),
            UnitInfo("Gallon", "gal", 0.264172),
            UnitInfo("Quart", "qt", 1.05669),
            UnitInfo("Pint", "pt", 2.11338),
            UnitInfo("Cup", "cup", 4.22675)
        ),
        UnitCategory.TIME to listOf(
            UnitInfo("Second", "s", 1.0),
            UnitInfo("Millisecond", "ms", 1000.0),
            UnitInfo("Minute", "min", 1.0/60.0),
            UnitInfo("Hour", "h", 1.0/3600.0),
            UnitInfo("Day", "d", 1.0/86400.0),
            UnitInfo("Week", "wk", 1.0/604800.0)
        ),
        UnitCategory.STORAGE to listOf(
            UnitInfo("Byte", "B", 1.0),
            UnitInfo("Kilobyte", "KB", 1.0/1024.0),
            UnitInfo("Megabyte", "MB", 1.0/(1024.0*1024.0)),
            UnitInfo("Gigabyte", "GB", 1.0/(1024.0*1024.0*1024.0)),
            UnitInfo("Terabyte", "TB", 1.0/(1024.0*1024.0*1024.0*1024.0))
        ),
        UnitCategory.ENERGY to listOf(
            UnitInfo("Joule", "J", 1.0),
            UnitInfo("Kilojoule", "kJ", 0.001),
            UnitInfo("Calorie", "cal", 0.239006),
            UnitInfo("Kilocalorie", "kcal", 0.000239006),
            UnitInfo("Watt-hour", "Wh", 0.000277778),
            UnitInfo("Kilowatt-hour", "kWh", 2.77778e-7)
        ),
        UnitCategory.PRESSURE to listOf(
            UnitInfo("Pascal", "Pa", 1.0),
            UnitInfo("Kilopascal", "kPa", 0.001),
            UnitInfo("Bar", "bar", 0.00001),
            UnitInfo("PSI", "psi", 0.000145038),
            UnitInfo("Atmosphere", "atm", 9.86923e-6)
        ),
        UnitCategory.ELECTRICAL to listOf(
            UnitInfo("Ampere", "A", 1.0),
            UnitInfo("Milliampere", "mA", 1000.0),
            UnitInfo("Microampere", "µA", 1000000.0),
            UnitInfo("Volt", "V", 1.0),
            UnitInfo("Ohm", "Ω", 1.0)
        ),
        UnitCategory.NUMBERS to listOf(
            UnitInfo("Decimal", "Dec", 10.0),
            UnitInfo("Binary", "Bin", 2.0),
            UnitInfo("Octal", "Oct", 8.0),
            UnitInfo("Hexadecimal", "Hex", 16.0),
            UnitInfo("Base N", "Base N", 32.0)
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
                    UnitInfo(getCurrencyName(rate.code), rate.code, 1.0, rate.rate)
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
                        val order = savedOrders.find { it.unitName == unit.name }?.displayOrder ?: 999
                        unit.copy(displayOrder = order)
                    }.sortedBy { it.displayOrder }
                }

                _uiState.update { state ->
                    val currentFromUnit = state.fromUnit
                    val newFromUnit = if (currentFromUnit.isEmpty() || orderedUnits.none { it.name == currentFromUnit }) {
                        if (category == UnitCategory.CURRENCY) {
                            orderedUnits.find { it.symbol == "USD" }?.name ?: orderedUnits.firstOrNull()?.name ?: ""
                        } else {
                            orderedUnits.firstOrNull()?.name ?: ""
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
        viewModelScope.launch {
            _inputFlow.emit(value)
        }
    }

    fun onFromUnitChange(unitName: String) {
        _uiState.update { it.copy(fromUnit = unitName) }
        performConversion()
    }
    
    fun onCustomBaseChange(base: String) {
        _uiState.update { it.copy(customBase = base) }
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
                UnitOrder(currentState.selectedCategory.name, it.name, it.displayOrder)
            }
            unitOrderRepository.saveOrders(orders)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun performConversion() {
        val currentState = _uiState.value
        val inputStr = currentState.inputValue
        val category = currentState.selectedCategory
        val units = currentState.availableUnits
        
        if (units.isEmpty() && category != UnitCategory.CURRENCY) return
        
        val fromUnit = units.find { it.name == currentState.fromUnit } ?: units.firstOrNull() ?: return

        if (category == UnitCategory.NUMBERS) {
            if (inputStr.isEmpty()) {
                val results = units.map { u -> ConversionResult(u.name, "0", u.symbol) }
                _uiState.update { it.copy(results = results) }
                return
            }

            val fromRadix = when (fromUnit.name) {
                "Binary" -> 2
                "Octal" -> 8
                "Hexadecimal" -> 16
                "Base N" -> currentState.customBase.toIntOrNull()?.coerceIn(2, 36) ?: 10
                else -> 10
            }

            val decimalValue = try {
                inputStr.toLong(fromRadix)
            } catch (e: Exception) {
                null
            }

            if (decimalValue != null) {
                val results = units.map { toUnit ->
                    val toRadix = when (toUnit.name) {
                        "Binary" -> 2
                        "Octal" -> 8
                        "Hexadecimal" -> 16
                        "Base N" -> currentState.customBase.toIntOrNull()?.coerceIn(2, 36) ?: 10
                        else -> 10
                    }
                    ConversionResult(
                        unitName = if (toUnit.name == "Base N") "Base ${currentState.customBase}" else toUnit.name,
                        value = try { decimalValue.toString(toRadix).uppercase() } catch (e: Exception) { "Error" },
                        symbol = if (toUnit.name == "Base N") "B${currentState.customBase}" else toUnit.symbol
                    )
                }
                _uiState.update { it.copy(results = results) }
            }
            return
        }

        val input = inputStr.toDoubleOrNull() ?: 0.0
        val baseValue = convertToBase(input, fromUnit, category)

        val results = units.map { toUnit ->
            val convertedValue = convertFromBase(baseValue, toUnit, category)
            ConversionResult(
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
            UnitCategory.TEMPERATURE -> {
                when (unit.name) {
                    "Celsius" -> value
                    "Fahrenheit" -> (value - 32) * 5 / 9
                    "Kelvin" -> value - 273.15
                    else -> value
                }
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
            UnitCategory.TEMPERATURE -> {
                when (unit.name) {
                    "Celsius" -> baseValue
                    "Fahrenheit" -> (baseValue * 9 / 5) + 32
                    "Kelvin" -> baseValue + 273.15
                    else -> baseValue
                }
            }
            UnitCategory.CURRENCY -> {
                baseValue * (unit.rate ?: 1.0)
            }
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

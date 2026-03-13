package com.example.aioapp.ui.paymentcomparator

data class InstallmentBreakdown(
    val month: Int,
    val nominalAmount: Double,
    val realAmount: Double
)

data class PaymentComparisonResult(
    val price: Double,
    val installments: Int,
    val monthlyRate: Double,
    val tea: Double,
    val installmentAmount: Double,
    val totalNominalCost: Double,
    val nominalSurcharge: Double,
    /** null when no inflation rate was provided by the user. */
    val totalRealCost: Double?,
    val realSurcharge: Double?,
    val breakdown: List<InstallmentBreakdown>,
    /** null when inflation was not provided — cannot determine advantage without it. */
    val financingIsAdvantageous: Boolean?
)

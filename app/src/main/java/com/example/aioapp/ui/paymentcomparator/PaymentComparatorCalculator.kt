package com.example.aioapp.ui.paymentcomparator

import kotlin.math.pow

/**
 * Pure Kotlin domain object for payment comparison calculations.
 * Contains no Android dependencies and is fully unit-testable.
 */
object PaymentComparatorCalculator {

    /** Converts TNA (Nominal Annual Rate) to a monthly proportional rate. */
    fun tnaToMonthlyRate(tna: Double): Double = tna / 12.0

    /**
     * Converts a monthly rate to TEA (Effective Annual Rate) using
     * compound interest: TEA = (1 + r_monthly)^12 - 1
     */
    fun monthlyRateToTea(monthlyRate: Double): Double = (1.0 + monthlyRate).pow(12) - 1.0

    /**
     * Computes the fixed monthly installment using the French amortization formula:
     *   installment = P * r / (1 - (1 + r)^(-n))
     *
     * Falls back to simple division when the rate is zero (interest-free plan).
     */
    fun computeInstallment(price: Double, n: Int, monthlyRate: Double): Double {
        if (monthlyRate == 0.0) return price / n
        return price * monthlyRate / (1.0 - (1.0 + monthlyRate).pow(-n))
    }

    /**
     * Builds the full [PaymentComparisonResult] for the given inputs.
     *
     * @param price             Base product price (must be > 0).
     * @param installments      Number of installments (must be >= 1).
     * @param tna               Nominal Annual Rate as a decimal (e.g. 0.60 for 60%).
     * @param monthlyInflation  Optional monthly inflation rate as a decimal (e.g. 0.05 for 5%).
     *                           Pass null or 0 to skip inflation-adjusted analysis.
     */
    fun compare(
        price: Double,
        installments: Int,
        tna: Double,
        monthlyInflation: Double? = null
    ): PaymentComparisonResult {
        require(price > 0) { "Price must be positive" }
        require(installments >= 1) { "Installments must be at least 1" }
        require(tna >= 0) { "TNA must be non-negative" }

        val r = tnaToMonthlyRate(tna)
        val tea = monthlyRateToTea(r)
        val installmentAmount = computeInstallment(price, installments, r)
        val totalNominalCost = installmentAmount * installments
        val nominalSurcharge = totalNominalCost - price

        val inflation = if (monthlyInflation != null && monthlyInflation > 0) monthlyInflation else null

        val breakdown = (1..installments).map { month ->
            val real = if (inflation != null) {
                installmentAmount / (1.0 + inflation).pow(month)
            } else {
                installmentAmount
            }
            InstallmentBreakdown(month, installmentAmount, real)
        }

        val totalRealCost = inflation?.let { breakdown.sumOf { it.realAmount } }
        val realSurcharge = totalRealCost?.let { it - price }
        val financingIsAdvantageous = totalRealCost?.let { it < price }

        return PaymentComparisonResult(
            price = price,
            installments = installments,
            monthlyRate = r,
            tea = tea,
            installmentAmount = installmentAmount,
            totalNominalCost = totalNominalCost,
            nominalSurcharge = nominalSurcharge,
            totalRealCost = totalRealCost,
            realSurcharge = realSurcharge,
            breakdown = breakdown,
            financingIsAdvantageous = financingIsAdvantageous
        )
    }
}

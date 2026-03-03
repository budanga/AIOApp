package com.example.aioapp.paymentcomparator

import com.example.aioapp.ui.paymentcomparator.PaymentComparatorCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PaymentComparatorCalculatorTest {

    private val epsilon = 0.0001

    // ── Rate conversions ──────────────────────────────────────────────────────

    @Test
    fun `tnaToMonthlyRate divides by 12`() {
        assertEquals(0.05, PaymentComparatorCalculator.tnaToMonthlyRate(0.60), epsilon)
        assertEquals(0.0, PaymentComparatorCalculator.tnaToMonthlyRate(0.0), epsilon)
    }

    @Test
    fun `monthlyRateToTea uses compound formula`() {
        // TNA=12% → monthly=1% → TEA=(1.01)^12-1 ≈ 12.6825%
        val expected = (1.01).pow(12) - 1.0
        assertEquals(expected, PaymentComparatorCalculator.monthlyRateToTea(0.01), epsilon)
    }

    @Test
    fun `monthlyRateToTea with zero rate returns zero`() {
        assertEquals(0.0, PaymentComparatorCalculator.monthlyRateToTea(0.0), epsilon)
    }

    // ── Installment calculation ───────────────────────────────────────────────

    @Test
    fun `computeInstallment with zero rate returns price divided by n`() {
        assertEquals(1000.0, PaymentComparatorCalculator.computeInstallment(12000.0, 12, 0.0), epsilon)
    }

    @Test
    fun `computeInstallment follows french amortization formula`() {
        // P=100000, n=12, r=0.05 (TNA=60%)
        val r = 0.05
        val n = 12
        val price = 100_000.0
        val expected = price * r / (1.0 - (1.0 + r).pow(-n))
        assertEquals(expected, PaymentComparatorCalculator.computeInstallment(price, n, r), epsilon)
    }

    // ── Nominal result ────────────────────────────────────────────────────────

    @Test
    fun `compare total nominal cost equals installment times n`() {
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 0.60)
        assertEquals(result.installmentAmount * 12, result.totalNominalCost, epsilon)
    }

    @Test
    fun `compare nominal surcharge equals total minus price`() {
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 0.60)
        assertEquals(result.totalNominalCost - result.price, result.nominalSurcharge, epsilon)
    }

    @Test
    fun `compare with zero TNA has no surcharge`() {
        val result = PaymentComparatorCalculator.compare(120_000.0, 12, 0.0)
        assertEquals(0.0, result.nominalSurcharge, epsilon)
        assertEquals(10_000.0, result.installmentAmount, epsilon)
    }

    // ── Inflation-adjusted result ─────────────────────────────────────────────

    @Test
    fun `compare without inflation returns null real fields`() {
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 0.60)
        assertNull(result.totalRealCost)
        assertNull(result.realSurcharge)
        assertNull(result.financingIsAdvantageous)
    }

    @Test
    fun `compare with zero inflation is treated as no inflation`() {
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 0.60, 0.0)
        assertNull(result.totalRealCost)
    }

    @Test
    fun `compare with high inflation makes financing advantageous`() {
        // At 100% annual rate (TNA=100%) and 20% monthly inflation,
        // inflation massively discounts later installments — real cost < spot price.
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 1.0, 0.20)
        assertNotNull(result.totalRealCost)
        assertTrue(result.financingIsAdvantageous == true)
    }

    @Test
    fun `compare with negligible inflation makes single payment cheaper`() {
        // 0% inflation, high TNA: real = nominal, financing always costs more
        val result = PaymentComparatorCalculator.compare(100_000.0, 12, 0.60, 0.001)
        assertNotNull(result.totalRealCost)
        assertFalse(result.financingIsAdvantageous == true)
    }

    @Test
    fun `breakdown count matches installments`() {
        val result = PaymentComparatorCalculator.compare(50_000.0, 6, 0.36, 0.05)
        assertEquals(6, result.breakdown.size)
        result.breakdown.forEachIndexed { index, item ->
            assertEquals(index + 1, item.month)
        }
    }

    @Test
    fun `breakdown real values decrease over time with positive inflation`() {
        val result = PaymentComparatorCalculator.compare(100_000.0, 6, 0.36, 0.05)
        val reals = result.breakdown.map { it.realAmount }
        for (i in 0 until reals.size - 1) {
            assertTrue("real[${i}] should be > real[${i+1}]", reals[i] > reals[i + 1])
        }
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `compare throws on zero price`() {
        PaymentComparatorCalculator.compare(0.0, 12, 0.60)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compare throws on negative price`() {
        PaymentComparatorCalculator.compare(-1.0, 12, 0.60)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compare throws on zero installments`() {
        PaymentComparatorCalculator.compare(100_000.0, 0, 0.60)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compare throws on negative TNA`() {
        PaymentComparatorCalculator.compare(100_000.0, 12, -0.01)
    }

    // ── Single installment edge case ──────────────────────────────────────────

    @Test
    fun `compare with one installment equals spot price when zero TNA`() {
        val result = PaymentComparatorCalculator.compare(50_000.0, 1, 0.0)
        assertEquals(50_000.0, result.totalNominalCost, epsilon)
        assertEquals(0.0, result.nominalSurcharge, epsilon)
    }
}

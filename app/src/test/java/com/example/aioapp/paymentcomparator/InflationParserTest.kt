package com.example.aioapp.paymentcomparator

import com.example.aioapp.core.repository.parseLatestInflation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class InflationParserTest {

    @Test
    fun `returns the most recent month even if the series is unordered`() {
        val json = """
            [{"fecha":"2026-09-30","valor":1.9},
             {"fecha":"2026-10-31","valor":2.2},
             {"fecha":"2026-08-31","valor":1.7}]
        """.trimIndent()

        val result = parseLatestInflation(json)

        assertEquals(2.2, result.percent, 0.0001)
        assertEquals(YearMonth.of(2026, 10), result.month)
    }

    @Test(expected = IllegalStateException::class)
    fun `empty series fails`() {
        parseLatestInflation("[]")
    }

    @Test(expected = Exception::class)
    fun `malformed body fails`() {
        parseLatestInflation("<html>error</html>")
    }
}

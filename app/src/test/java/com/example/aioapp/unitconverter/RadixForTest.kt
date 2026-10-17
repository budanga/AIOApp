package com.example.aioapp.unitconverter

import com.example.aioapp.ui.unitconverter.radixFor
import org.junit.Assert.assertEquals
import org.junit.Test

class RadixForTest {

    @Test
    fun `fixed bases are resolved by unit id`() {
        assertEquals(10, radixFor("decimal", "32"))
        assertEquals(2, radixFor("binary", "32"))
        assertEquals(8, radixFor("octal", "32"))
        assertEquals(16, radixFor("hexadecimal", "32"))
    }

    @Test
    fun `display names are not treated as ids`() {
        // Regression: the hex keyboard compared the display name against the stored id
        assertEquals(10, radixFor("Hexadecimal", "32"))
    }

    @Test
    fun `custom base is clamped to the supported range`() {
        assertEquals(32, radixFor("base_n", "32"))
        assertEquals(36, radixFor("base_n", "99"))
        assertEquals(2, radixFor("base_n", "1"))
        assertEquals(10, radixFor("base_n", ""))
    }
}

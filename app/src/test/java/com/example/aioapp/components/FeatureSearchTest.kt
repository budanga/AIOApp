package com.example.aioapp.components

import com.example.aioapp.ui.components.matchesSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureSearchTest {

    @Test
    fun `blank query matches everything`() {
        assertTrue(matchesSearch("Notas", ""))
        assertTrue(matchesSearch("Notas", "   "))
    }

    @Test
    fun `match ignores case and surrounding spaces`() {
        assertTrue(matchesSearch("Conversor de unidades", " CONVERSOR "))
    }

    @Test
    fun `match ignores accents in both name and query`() {
        assertTrue(matchesSearch("Calculadora de Asado", "calculadóra"))
        assertTrue(matchesSearch("Gestión de archivos", "gestion"))
    }

    @Test
    fun `match finds words in the middle of the name`() {
        assertTrue(matchesSearch("Gestor de archivos", "archiv"))
    }

    @Test
    fun `non matching query is rejected`() {
        assertFalse(matchesSearch("Buscaminas", "truco"))
    }
}

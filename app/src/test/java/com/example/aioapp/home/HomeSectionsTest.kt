package com.example.aioapp.home

import com.example.aioapp.core.model.FeatureCategory
import com.example.aioapp.core.model.functionalities
import com.example.aioapp.core.model.homeSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionsTest {

    @Test
    fun `no favorites leaves the favorites row empty`() {
        val sections = homeSections(emptyList())
        assertTrue(sections.favorites.isEmpty())
    }

    @Test
    fun `favorites keep the most recently favorited first`() {
        val sections = homeSections(listOf("truco", "notes"))
        assertEquals(listOf("truco", "notes"), sections.favorites.map { it.route })
    }

    @Test
    fun `favorites still appear in their category, in default order`() {
        val sections = homeSections(listOf("pomodoro"))
        val tools = sections.byCategory.getValue(FeatureCategory.Tools).map { it.route }
        assertEquals(listOf("filemanager", "notes", "pomodoro", "unitconverter", "paymentcomparator", "bbq"), tools)
    }

    @Test
    fun `games go to leisure`() {
        val leisure = homeSections(emptyList()).byCategory.getValue(FeatureCategory.Leisure).map { it.route }
        assertEquals(listOf("truco", "minesweeper"), leisure)
    }

    @Test
    fun `every feature lands in exactly one category`() {
        val all = homeSections(emptyList()).byCategory.values.flatten().map { it.route }
        assertEquals(functionalities.map { it.route }.sorted(), all.sorted())
    }

    @Test
    fun `unknown stored routes are ignored`() {
        val sections = homeSections(listOf("removed_feature", "notes"))
        assertEquals(listOf("notes"), sections.favorites.map { it.route })
    }
}

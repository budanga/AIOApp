package com.example.aioapp.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

const val PACKAGE_NAME = "com.example.aioapp"

private const val TIMEOUT_MS = 5_000L

/** Feature names in Spanish and English, so the journeys work whatever the device language is. */
private val features = listOf(
    "Gestor de archivos" to "File Manager",
    "Notas" to "Notes",
    "Pomodoro" to "Pomodoro",
    "Conversor de unidades" to "Unit Converter",
    "Truco" to "Truco",
    "Comparador de pagos" to "Payment Comparator",
    "Buscaminas" to "Minesweeper",
    "Calculadora de Asado" to "BBQ Calculator",
)

fun MacrobenchmarkScope.waitForHome() {
    device.wait(Until.hasObject(By.text(namePattern(features.first()))), TIMEOUT_MS)
}

/** Opens every feature from the home grid, scrolls where there is a list, and goes back. */
fun MacrobenchmarkScope.visitAllFeatures() {
    for (feature in features) {
        val card = findOnHome(feature) ?: continue
        card.click()
        device.waitForIdle()
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            list.fling(Direction.UP)
        }
        device.pressBack()
        waitForHome()
    }
}

private fun MacrobenchmarkScope.findOnHome(feature: Pair<String, String>) =
    device.findObject(By.text(namePattern(feature)))
        ?: run {
            // Lower cards may be off screen on small displays
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f)
            device.wait(Until.findObject(By.text(namePattern(feature))), TIMEOUT_MS)
        }

private fun namePattern(feature: Pair<String, String>): Pattern =
    Pattern.compile("${Pattern.quote(feature.first)}|${Pattern.quote(feature.second)}")

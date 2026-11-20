package com.example.aioapp.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold startup and frame timing while visiting every feature, with and without the
 * Baseline Profile. Run with: ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    @Test
    fun startupWithProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun featuresWithoutProfile() = features(CompilationMode.None())

    @Test
    fun featuresWithProfile() = features(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        waitForHome()
    }

    private fun features(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 3,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        waitForHome()
        visitAllFeatures()
    }
}

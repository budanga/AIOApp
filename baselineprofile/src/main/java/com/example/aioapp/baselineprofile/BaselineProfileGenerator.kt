package com.example.aioapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of startup and of opening every feature, so they ship precompiled
 * instead of running interpreted/JIT on first use. Run with: ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        waitForHome()
        visitAllFeatures()
    }
}

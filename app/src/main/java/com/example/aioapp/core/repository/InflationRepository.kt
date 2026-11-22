package com.example.aioapp.core.repository

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Latest official monthly inflation (INDEC CPI), as a percentage (2.2 = 2.2%). */
data class MonthlyInflation(val percent: Double, val month: YearMonth)

interface InflationRepository {
    suspend fun latestMonthlyInflation(): Result<MonthlyInflation>
}

private const val INFLATION_URL = "https://api.argentinadatos.com/v1/finanzas/indices/inflacion"
private const val TIMEOUT_MS = 10_000

@Singleton
class InflationRepositoryImpl @Inject constructor() : InflationRepository {

    override suspend fun latestMonthlyInflation(): Result<MonthlyInflation> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(INFLATION_URL).openConnection() as HttpURLConnection
            try {
                // Without timeouts a flaky connection would leave the field "loading" indefinitely
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                parseLatestInflation(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
    }
}

/** Parses `[{"fecha":"2026-10-31","valor":2.2}, ...]` and returns the most recent month. */
internal fun parseLatestInflation(json: String): MonthlyInflation {
    // Gson's tree API instead of org.json: the latter is a stub in JVM unit tests
    val latest = JsonParser.parseString(json).asJsonArray
        .map { it.asJsonObject }
        .maxByOrNull { it.get("fecha").asString }
        ?: throw IllegalStateException("Inflation series is empty")
    return MonthlyInflation(
        percent = latest.get("valor").asDouble,
        month = YearMonth.from(LocalDate.parse(latest.get("fecha").asString))
    )
}

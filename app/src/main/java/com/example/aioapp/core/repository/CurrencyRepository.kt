package com.example.aioapp.core.repository

import com.example.aioapp.core.database.CurrencyDao
import com.example.aioapp.core.model.CurrencyRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface CurrencyRepository {
    fun getRates(): Flow<List<CurrencyRate>>
    suspend fun refreshRates(): Result<Unit>
    suspend fun getLastUpdated(): Long?
}

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao
) : CurrencyRepository {

    override fun getRates(): Flow<List<CurrencyRate>> = currencyDao.getAllRates()

    override suspend fun getLastUpdated(): Long? = currencyDao.getLastUpdated()

    override suspend fun refreshRates(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = URL("https://open.er-api.com/v6/latest/USD").readText()
            val json = JSONObject(response)
            val ratesJson = json.getJSONObject("rates")
            val timeLastUpdate = json.getLong("time_last_update_unix") * 1000

            val rates = mutableListOf<CurrencyRate>()
            val keys = ratesJson.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                rates.add(CurrencyRate(code, ratesJson.getDouble(code), timeLastUpdate))
            }
            
            currencyDao.insertRates(rates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.aioapp.core.network

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String = "USD"): CurrencyResponse
}

data class CurrencyResponse(
    val result: String,
    val time_last_update_unix: Long,
    val base_code: String,
    val rates: Map<String, Double>
)

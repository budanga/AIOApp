package com.example.aioapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.paymentDataStore: DataStore<Preferences> by preferencesDataStore(name = "payment_comparator_prefs")

data class PaymentComparatorPrefs(
    val price: String = "",
    val installments: Int = 12,
    val tna: String = "",
    val inflation: String = ""
)

@Singleton
class PaymentComparatorPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRICE = stringPreferencesKey("price")
        val INSTALLMENTS = intPreferencesKey("installments")
        val TNA = stringPreferencesKey("tna")
        val INFLATION = stringPreferencesKey("inflation")
    }

    val paymentPrefsFlow: Flow<PaymentComparatorPrefs> = context.paymentDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PaymentComparatorPrefs(
                price = preferences[Keys.PRICE] ?: "",
                installments = preferences[Keys.INSTALLMENTS] ?: 12,
                tna = preferences[Keys.TNA] ?: "",
                inflation = preferences[Keys.INFLATION] ?: ""
            )
        }

    suspend fun savePrice(price: String) {
        context.paymentDataStore.edit { preferences ->
            preferences[Keys.PRICE] = price
        }
    }

    suspend fun saveInstallments(installments: Int) {
        context.paymentDataStore.edit { preferences ->
            preferences[Keys.INSTALLMENTS] = installments
        }
    }

    suspend fun saveTna(tna: String) {
        context.paymentDataStore.edit { preferences ->
            preferences[Keys.TNA] = tna
        }
    }

    suspend fun saveInflation(inflation: String) {
        context.paymentDataStore.edit { preferences ->
            preferences[Keys.INFLATION] = inflation
        }
    }

    suspend fun saveAll(price: String, installments: Int, tna: String, inflation: String) {
        context.paymentDataStore.edit { preferences ->
            preferences[Keys.PRICE] = price
            preferences[Keys.INSTALLMENTS] = installments
            preferences[Keys.TNA] = tna
            preferences[Keys.INFLATION] = inflation
        }
    }
}

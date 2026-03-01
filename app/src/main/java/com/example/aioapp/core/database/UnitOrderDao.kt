package com.example.aioapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aioapp.core.model.UnitOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitOrderDao {
    @Query("SELECT * FROM unit_orders WHERE category = :category")
    fun getOrdersByCategory(category: String): Flow<List<UnitOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<UnitOrder>)
}

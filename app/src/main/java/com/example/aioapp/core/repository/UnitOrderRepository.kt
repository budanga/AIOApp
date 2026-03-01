package com.example.aioapp.core.repository

import com.example.aioapp.core.database.UnitOrderDao
import com.example.aioapp.core.model.UnitOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface UnitOrderRepository {
    fun getOrdersByCategory(category: String): Flow<List<UnitOrder>>
    suspend fun saveOrders(orders: List<UnitOrder>)
}

@Singleton
class UnitOrderRepositoryImpl @Inject constructor(
    private val unitOrderDao: UnitOrderDao
) : UnitOrderRepository {
    override fun getOrdersByCategory(category: String): Flow<List<UnitOrder>> =
        unitOrderDao.getOrdersByCategory(category)

    override suspend fun saveOrders(orders: List<UnitOrder>) =
        unitOrderDao.insertOrders(orders)
}

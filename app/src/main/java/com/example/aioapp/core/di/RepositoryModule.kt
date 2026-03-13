package com.example.aioapp.core.di

import com.example.aioapp.core.repository.CurrencyRepository
import com.example.aioapp.core.repository.CurrencyRepositoryImpl
import com.example.aioapp.core.repository.UnitOrderRepository
import com.example.aioapp.core.repository.UnitOrderRepositoryImpl
import com.example.aioapp.core.repository.TrucoRepository
import com.example.aioapp.core.repository.TrucoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(
        currencyRepositoryImpl: CurrencyRepositoryImpl
    ): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindUnitOrderRepository(
        unitOrderRepositoryImpl: UnitOrderRepositoryImpl
    ): UnitOrderRepository

    @Binds
    @Singleton
    abstract fun bindTrucoRepository(
        trucoRepositoryImpl: TrucoRepositoryImpl
    ): TrucoRepository
}

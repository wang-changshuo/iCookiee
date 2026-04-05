package com.aifinance.core.data.di

import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.AccountRepositoryImpl
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.CategoryRepositoryImpl
import com.aifinance.core.data.repository.ScheduledRuleRepository
import com.aifinance.core.data.repository.ScheduledRuleRepositoryImpl
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.repository.TransactionRepositoryImpl
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
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindScheduledRuleRepository(
        impl: ScheduledRuleRepositoryImpl
    ): ScheduledRuleRepository

}

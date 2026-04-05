package com.aifinance.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.dao.CategoryDao
import com.aifinance.core.database.dao.ScheduledRuleDao
import com.aifinance.core.database.dao.TransactionDao
import com.aifinance.core.database.entity.AccountEntity
import com.aifinance.core.database.entity.CategoryEntity
import com.aifinance.core.database.entity.ScheduledRuleEntity
import com.aifinance.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ScheduledRuleEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AiFinanceDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledRuleDao(): ScheduledRuleDao
}

package com.aifinance.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.dao.CategoryDao
import com.aifinance.core.database.dao.ScheduledRuleDao
import com.aifinance.core.database.dao.TransactionDao
import com.aifinance.core.database.entity.AccountEntity
import com.aifinance.core.database.entity.CategoryEntity
import com.aifinance.core.database.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Provider

class DatabaseCallback(
    private val accountDao: Provider<AccountDao>,
    private val categoryDao: Provider<CategoryDao>,
    private val transactionDao: Provider<TransactionDao>,
    private val scheduledRuleDao: Provider<ScheduledRuleDao>,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        CoroutineScope(Dispatchers.IO).launch {
            val accountId = UUID.randomUUID()
            val initialBalance = BigDecimal("10000.00")
            val account = AccountEntity(
                id = accountId,
                name = "测试现金账户",
                type = "CASH",
                currency = "CNY",
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                color = 0xFF4CAF50.toInt(),
                icon = "wallet",
                note = "",
                isArchived = false,
                includeInTotalAssets = true,
                isDefaultIncomeExpense = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            accountDao.get().insert(account)
            // Manually adjust balance for seed transaction since we bypass repository
            accountDao.get().adjustCurrentBalance(accountId, BigDecimal("-150.00"))

            val categoryId = UUID.randomUUID()
            val category = CategoryEntity(
                id = categoryId,
                name = "餐饮美食",
                icon = "restaurant",
                color = 0xFFE91E63.toInt(),
                type = "EXPENSE",
                parentId = null,
                isDefault = false,
                order = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            categoryDao.get().insert(category)

            val transaction = TransactionEntity(
                id = UUID.randomUUID(),
                accountId = accountId,
                categoryId = categoryId,
                type = "EXPENSE",
                amount = BigDecimal("150.00"),
                currency = "CNY",
                title = "午餐 - 麦当劳",
                description = "测试数据：工作午餐",
                date = LocalDate.now(),
                time = Instant.now(),
                isPending = false,
                receiptImagePath = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                sourceType = "MANUAL",
                importBatchId = null,
                rawText = null,
                aiCategory = null,
                aiConfidence = null,
                userConfirmed = true,
                ocrSourceId = null,
                paymentMethod = "支付宝",
                paymentAccount = "花呗",
            )
            transactionDao.get().insert(transaction)
        }
    }
}

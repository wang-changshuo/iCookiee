package com.aifinance.core.data.repository

import android.util.Log
import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.dao.TransactionDao
import com.aifinance.core.database.entity.TransactionEntity
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByAccount(accountId: UUID): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: UUID): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        applyBalanceImpact(transaction, direction = 1)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val oldTransaction = transactionDao.getTransactionById(transaction.id)?.toDomain()
        if (oldTransaction != null) {
            applyBalanceImpact(oldTransaction, direction = -1)
        }
        transactionDao.update(transaction.toEntity())
        applyBalanceImpact(transaction, direction = 1)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val existing = transactionDao.getTransactionById(transaction.id)?.toDomain() ?: transaction
        transactionDao.delete(existing.toEntity())
        applyBalanceImpact(existing, direction = -1)
    }

    override suspend fun clearAllTransactionHistory() {
        transactionDao.deleteAllTransactions()
        accountDao.clearAllBalancesToZero()
    }

    private suspend fun applyBalanceImpact(transaction: Transaction, direction: Int) {
        val baseDelta = when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> transaction.amount.negate()
            TransactionType.TRANSFER -> when {
                transaction.title.startsWith("转出") -> transaction.amount.negate()
                transaction.title.startsWith("转入") -> transaction.amount
                else -> transaction.amount.negate()
            }
        }

        if (baseDelta.compareTo(BigDecimal.ZERO) != 0) {
            val signedDelta = baseDelta.multiply(BigDecimal.valueOf(direction.toLong()))
            Log.d(
                "TransactionRepo",
                "Adjusting balance for account ${transaction.accountId}: " +
                    "type=${transaction.type}, amount=${transaction.amount}, " +
                    "delta=$signedDelta, direction=$direction"
            )
            accountDao.adjustCurrentBalance(transaction.accountId, signedDelta)
        } else {
            Log.w(
                "TransactionRepo",
                "Skipping balance adjustment for transaction ${transaction.id}: delta is zero"
            )
        }
    }
}

private fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = TransactionType.valueOf(type),
        amount = amount,
        currency = currency,
        title = title,
        description = description,
        date = date,
        time = time,
        isPending = isPending,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceType = TransactionSourceType.valueOf(sourceType),
        importBatchId = importBatchId,
        rawText = rawText,
        aiCategory = aiCategory,
        aiConfidence = aiConfidence,
        userConfirmed = userConfirmed,
        ocrSourceId = ocrSourceId,
        paymentMethod = paymentMethod,
        paymentAccount = paymentAccount,
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = type.name,
        amount = amount,
        currency = currency,
        title = title,
        description = description,
        date = date,
        time = time,
        isPending = isPending,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceType = sourceType.name,
        importBatchId = importBatchId,
        rawText = rawText,
        aiCategory = aiCategory,
        aiConfidence = aiConfidence,
        userConfirmed = userConfirmed,
        ocrSourceId = ocrSourceId,
        paymentMethod = paymentMethod,
        paymentAccount = paymentAccount,
    )
}

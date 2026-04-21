package com.aifinance.core.data.repository

import com.aifinance.core.model.Category
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: UUID): Flow<List<Transaction>>
    suspend fun getTransactionById(id: UUID): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun clearAllTransactionHistory()
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>
    suspend fun getCategoryById(id: UUID): Category?
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

interface AccountRepository {
    fun getActiveAccounts(): Flow<List<com.aifinance.core.model.Account>>
    suspend fun getAccountById(id: UUID): com.aifinance.core.model.Account?
    suspend fun getDefaultIncomeExpenseAccount(): com.aifinance.core.model.Account?
    suspend fun getFirstActiveAccount(): com.aifinance.core.model.Account?
    suspend fun insertAccount(account: com.aifinance.core.model.Account)
    suspend fun updateAccount(account: com.aifinance.core.model.Account)
    suspend fun deleteAccount(account: com.aifinance.core.model.Account)
}

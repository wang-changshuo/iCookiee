package com.aifinance.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val accounts: StateFlow<List<Account>> =
        accountRepository.getActiveAccounts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val categories: List<Category> = CategoryCatalog.allCategories()

    fun categoriesForType(type: TransactionType): List<Category> {
        return CategoryCatalog.categoriesForType(type)
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun updateTransactionCategory(transaction: Transaction, categoryId: UUID) {
        val category = CategoryCatalog.resolve(categoryId = categoryId, type = transaction.type).asCategory()
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    categoryId = category.id,
                    title = category.name,
                )
            )
        }
    }

    fun updateTransactionDetail(
        transaction: Transaction,
        amount: BigDecimal,
        accountId: UUID,
        date: LocalDate,
        type: TransactionType,
        includeInExpense: Boolean,
    ) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    amount = amount,
                    accountId = accountId,
                    date = date,
                    type = type,
                    isPending = !includeInExpense,
                )
            )
        }
    }

    fun updateTransactionEditor(
        transaction: Transaction,
        amount: BigDecimal,
        accountId: UUID,
        dateTime: LocalDateTime,
        type: TransactionType,
        remark: String?,
    ) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    amount = amount,
                    accountId = accountId,
                    date = dateTime.toLocalDate(),
                    time = AppDateTime.toInstant(dateTime),
                    type = type,
                    description = remark,
                )
            )
        }
    }
}

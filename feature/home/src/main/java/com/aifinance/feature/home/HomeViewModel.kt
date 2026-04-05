package com.aifinance.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.AccountType
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    val categoriesById: StateFlow<Map<UUID, Category>> =
        categoryRepository.getAllCategories()
            .map { customCategories ->
                buildMap {
                    CategoryCatalog.all.forEach { put(it.id, it.asCategory()) }
                    customCategories.forEach { put(it.id, it) }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap(),
            )

    fun getCategoriesForType(type: TransactionType): Flow<List<Category>> =
        categoryRepository.getAllCategories()
            .map { customCategories ->
                val defaults = CategoryCatalog.forType(type).map { it.asCategory() }
                val customForType = customCategories
                    .filter { it.type == type && !it.isDefault }
                    .sortedBy { it.order }
                defaults + customForType
            }

    val recentTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val monthlyStats: StateFlow<MonthlyStats> =
        transactionRepository.getAllTransactions()
            .map { transactions ->
                val now = LocalDate.now()
                val currentMonth = now.monthValue
                val currentYear = now.year

                val monthTransactions = transactions.filter {
                    it.date.monthValue == currentMonth && it.date.year == currentYear && !it.isPending
                }

                val income = monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                val expense = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

                MonthlyStats(income = income, expense = expense)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MonthlyStats(BigDecimal.ZERO, BigDecimal.ZERO)
            )

    val totalBalance: StateFlow<TotalBalance> =
        accountRepository.getActiveAccounts()
            .map { accounts ->
                val totalAssets = accounts
                    .filter { it.includeInTotalAssets && it.type != AccountType.CREDIT_CARD }
                    .fold(BigDecimal.ZERO) { acc, account -> acc + account.currentBalance }

                val totalLiabilities = accounts
                    .filter { it.includeInTotalAssets && it.type == AccountType.CREDIT_CARD }
                    .fold(BigDecimal.ZERO) { acc, account -> acc + account.currentBalance.abs() }

                val total = totalAssets - totalLiabilities
                TotalBalance(
                    balance = total,
                    currency = "CNY",
                    assets = totalAssets,
                    liabilities = totalLiabilities,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TotalBalance(
                    balance = BigDecimal.ZERO,
                    currency = "CNY",
                    assets = BigDecimal.ZERO,
                    liabilities = BigDecimal.ZERO,
                )
            )

    val accountsById: StateFlow<Map<UUID, Account>> =
        accountRepository.getActiveAccounts()
            .map { accounts -> accounts.associateBy { it.id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap(),
            )

    val monthOverMonthChange: StateFlow<PercentageChange> =
        transactionRepository.getAllTransactions()
            .map { transactions ->
                val now = LocalDate.now()
                val currentMonth = now.monthValue
                val currentYear = now.year
                val lastMonth = now.minusMonths(1)

                val currentMonthTotal = transactions
                    .filter { it.date.monthValue == currentMonth && it.date.year == currentYear && !it.isPending }
                    .fold(BigDecimal.ZERO) { acc, t ->
                        when (t.type) {
                            TransactionType.INCOME -> acc + t.amount
                            TransactionType.EXPENSE -> acc - t.amount
                            TransactionType.TRANSFER -> acc
                        }
                    }

                val lastMonthTotal = transactions
                    .filter { it.date.monthValue == lastMonth.monthValue && it.date.year == lastMonth.year && !it.isPending }
                    .fold(BigDecimal.ZERO) { acc, t ->
                        when (t.type) {
                            TransactionType.INCOME -> acc + t.amount
                            TransactionType.EXPENSE -> acc - t.amount
                            TransactionType.TRANSFER -> acc
                        }
                    }

                val percentage = if (lastMonthTotal.compareTo(BigDecimal.ZERO) != 0) {
                    currentMonthTotal.subtract(lastMonthTotal)
                        .divide(lastMonthTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }

                PercentageChange(
                    percentage = percentage,
                    isPositive = percentage >= BigDecimal.ZERO
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PercentageChange(BigDecimal.ZERO, true)
            )

    val weeklyInsight: StateFlow<WeeklyInsight?> =
        transactionRepository.getAllTransactions()
            .map { transactions ->
                calculateWeeklyInsight(transactions)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun updateTransactionCategory(
        transaction: Transaction,
        categoryId: UUID,
        categoryName: String? = null,
    ) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    categoryId = categoryId,
                    title = categoryName?.takeIf { it.isNotBlank() } ?: transaction.title,
                )
            )
        }
    }

    fun updateTransactionDetails(
        transaction: Transaction,
        editedFields: HomeTransactionEditedFields,
    ) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    amount = editedFields.amount,
                    accountId = editedFields.accountId,
                    date = editedFields.date,
                    type = editedFields.type,
                    title = editedFields.title?.takeIf { it.isNotBlank() } ?: transaction.title,
                    description = editedFields.description ?: transaction.description,
                    isPending = !editedFields.includeInExpense,
                )
            )
        }
    }
}

data class HomeTransactionEditedFields(
    val amount: BigDecimal,
    val accountId: UUID,
    val date: LocalDate,
    val type: TransactionType,
    val includeInExpense: Boolean,
    val title: String? = null,
    val description: String? = null,
)

data class MonthlyStats(
    val income: BigDecimal,
    val expense: BigDecimal
)

data class TotalBalance(
    val balance: BigDecimal,
    val currency: String,
    val assets: BigDecimal,
    val liabilities: BigDecimal,
)

data class PercentageChange(
    val percentage: BigDecimal,
    val isPositive: Boolean
)

data class WeeklyInsight(
    val title: String,
    val subtitle: String,
    val alertLevel: AlertLevel
)

enum class AlertLevel {
    NORMAL, WARNING, CRITICAL
}

private fun calculateWeeklyInsight(transactions: List<Transaction>): WeeklyInsight? {
    val now = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val currentWeek = now.get(weekFields.weekOfWeekBasedYear())
    val currentYear = now.get(weekFields.weekBasedYear())
    val lastWeek = now.minusWeeks(1)
    val lastWeekNumber = lastWeek.get(weekFields.weekOfWeekBasedYear())
    val lastWeekYear = lastWeek.get(weekFields.weekBasedYear())

    val currentWeekFoodExpense = transactions
        .filter {
            val week = it.date.get(weekFields.weekOfWeekBasedYear())
            val year = it.date.get(weekFields.weekBasedYear())
            week == currentWeek && year == currentYear &&
                    it.type == TransactionType.EXPENSE && !it.isPending
        }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

    val lastWeekFoodExpense = transactions
        .filter {
            val week = it.date.get(weekFields.weekOfWeekBasedYear())
            val year = it.date.get(weekFields.weekBasedYear())
            week == lastWeekNumber && year == lastWeekYear &&
                    it.type == TransactionType.EXPENSE && !it.isPending
        }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

    return when {
        currentWeekFoodExpense <= BigDecimal.ZERO -> null
        lastWeekFoodExpense <= BigDecimal.ZERO -> WeeklyInsight(
            title = "本周有新的支出记录",
            subtitle = "记得记录每一笔消费",
            alertLevel = AlertLevel.NORMAL
        )
        else -> {
            val increasePercentage = currentWeekFoodExpense
                .subtract(lastWeekFoodExpense)
                .divide(lastWeekFoodExpense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)

            if (increasePercentage >= BigDecimal(20)) {
                WeeklyInsight(
                    title = "本周支出 +${increasePercentage}%",
                    subtitle = "建议控制非必要支出",
                    alertLevel = AlertLevel.WARNING
                )
            } else {
                WeeklyInsight(
                    title = "本周支出正常",
                    subtitle = "继续保持良好的消费习惯",
                    alertLevel = AlertLevel.NORMAL
                )
            }
        }
    }
}

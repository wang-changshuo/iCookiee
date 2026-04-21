package com.aifinance.core.model

data class HomeState(
    val totalBalance: com.aifinance.core.model.MoneyAmount,
    val monthlyExpense: com.aifinance.core.model.MoneyAmount,
    val monthlyIncome: com.aifinance.core.model.MoneyAmount,
    val recentTransactions: List<Transaction>,
    val aiReminders: List<AIReminder>,
    val pendingImports: Int,
    val pendingConfirmations: Int,
    val isLoading: Boolean = false,
)

data class MoneyAmount(
    val amount: java.math.BigDecimal,
    val currency: CurrencyCode,
)

data class AIReminder(
    val id: String,
    val type: ReminderType,
    val title: String,
    val message: String,
    val actionLabel: String?,
    val priority: ReminderPriority = ReminderPriority.NORMAL,
)

enum class ReminderType {
    BUDGET_ALERT,
    UNUSUAL_SPENDING,
    IMPORT_READY,
    AI_SUGGESTION,
    BILL_DUE,
}

enum class ReminderPriority {
    LOW,
    NORMAL,
    HIGH,
}

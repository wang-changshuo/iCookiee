package com.aifinance.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.repository.ai.AIRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: String = "",
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: String? = null,
    val note: String = "",
    val selectedDate: LocalDate = AppDateTime.today(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    val aiRepository: AIRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val categories: StateFlow<List<Category>> = combine(
        _uiState,
        categoryRepository.getAllCategories()
    ) { state, customCategories ->
        val defaults = CategoryCatalog.categoriesForType(state.selectedType)
        val customForType = customCategories.filter { it.type == state.selectedType && !it.isDefault }
        defaults + customForType
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryCatalog.categoriesForType(TransactionType.EXPENSE)
    )

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun saveTransaction(
        amount: String,
        type: TransactionType,
        category: String?,
        note: String,
        dateTime: LocalDateTime,
        accountId: UUID,
        targetAccountId: UUID? = null,
    ) {
        if (amount.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val amountValue = BigDecimal(amount)
                if (type == TransactionType.TRANSFER && targetAccountId != null) {
                    val sourceAccount = accountRepository.getAccountById(accountId)
                    val targetAccount = accountRepository.getAccountById(targetAccountId)
                    val sourceName = sourceAccount?.name ?: "账户"
                    val targetName = targetAccount?.name ?: "账户"
                    val transferBatchId = UUID.randomUUID()
                    val noteText = note.ifEmpty { null }

                    val transferOut = Transaction(
                        accountId = accountId,
                        categoryId = null,
                        type = TransactionType.TRANSFER,
                        amount = amountValue,
                        currency = "CNY",
                        title = "转出至$targetName",
                        description = noteText,
                        date = dateTime.toLocalDate(),
                        time = AppDateTime.toInstant(dateTime),
                        sourceType = TransactionSourceType.MANUAL,
                        importBatchId = transferBatchId,
                    )
                    val transferIn = Transaction(
                        accountId = targetAccountId,
                        categoryId = null,
                        type = TransactionType.TRANSFER,
                        amount = amountValue,
                        currency = "CNY",
                        title = "转入自$sourceName",
                        description = noteText,
                        date = dateTime.toLocalDate(),
                        time = AppDateTime.toInstant(dateTime),
                        sourceType = TransactionSourceType.MANUAL,
                        importBatchId = transferBatchId,
                    )

                    transactionRepository.insertTransaction(transferOut)
                    transactionRepository.insertTransaction(transferIn)
                } else {
                    val categoryId = category?.let { categoryName ->
                        val catalogMatch = CategoryCatalog.forType(type)
                            .firstOrNull { it.name == categoryName }
                        if (catalogMatch != null) {
                            catalogMatch.id
                        } else {
                            categories.value.firstOrNull { it.name == categoryName }?.id
                        }
                    }

                    val transaction = Transaction(
                        accountId = accountId,
                        categoryId = categoryId,
                        type = type,
                        amount = amountValue,
                        currency = "CNY",
                        title = category ?: "未分类",
                        description = note.ifEmpty { null },
                        date = dateTime.toLocalDate(),
                        time = AppDateTime.toInstant(dateTime),
                        sourceType = TransactionSourceType.MANUAL
                    )

                    transactionRepository.insertTransaction(transaction)
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun resetState() {
        _uiState.value = AddTransactionUiState()
    }

    suspend fun resolveDefaultAccountId(): UUID? {
        val defaultAccount = accountRepository.getDefaultIncomeExpenseAccount()
        if (defaultAccount != null) {
            return defaultAccount.id
        }

        return accountRepository.getFirstActiveAccount()?.id
    }
}

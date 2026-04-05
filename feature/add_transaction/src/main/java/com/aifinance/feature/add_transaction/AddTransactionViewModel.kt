package com.aifinance.feature.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.TransactionRepository
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
import java.util.UUID
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: String = "",
    val title: String = "",
    val categoryId: UUID? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = combine(
        _uiState,
        categoryRepository.getAllCategories()
    ) { state, customCategories ->
        mergeCategories(state.type, customCategories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryCatalog.categoriesForType(TransactionType.EXPENSE)
    )

    init {
        _uiState.value = AddTransactionUiState(
            categoryId = CategoryCatalog.fallback(TransactionType.EXPENSE).id,
        )
    }

    fun onAmountChanged(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val finalAmount = if (decimalCount > 1) {
            val firstDecimal = filtered.indexOf('.')
            filtered.filterIndexed { index, c ->
                c != '.' || index == firstDecimal
            }
        } else {
            filtered
        }
        _uiState.value = _uiState.value.copy(
            amount = finalAmount,
            amountError = null
        )
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onCategorySelected(categoryId: UUID) {
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId,
            categoryError = null
        )
    }

    fun onTypeChanged(type: TransactionType) {
        val categoryIdForType = _uiState.value.categoryId
            ?.takeIf { selectedId -> CategoryCatalog.resolve(selectedId, type).id == selectedId }
            ?: CategoryCatalog.fallback(type).id

        _uiState.value = _uiState.value.copy(
            type = type,
            categoryId = categoryIdForType,
            categoryError = null,
        )
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        var hasError = false
        var newState = currentState

        val amountValue = try {
            BigDecimal(currentState.amount.ifEmpty { "0" })
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }

        if (amountValue <= BigDecimal.ZERO) {
            newState = newState.copy(amountError = "金额必须大于0")
            hasError = true
        }

        if (currentState.categoryId == null) {
            newState = newState.copy(categoryError = "请选择分类")
            hasError = true
        }

        if (hasError) {
            _uiState.value = newState
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val defaultAccountId = accountRepository.getDefaultIncomeExpenseAccount()?.id
                    ?: accountRepository.getFirstActiveAccount()?.id
                    ?: UUID.randomUUID()

                val selectedCategory = CategoryCatalog.findById(currentState.categoryId)
                val autoTitle = currentState.title.takeIf { it.isNotBlank() }
                    ?: selectedCategory?.name
                    ?: "未分类"

                val transaction = Transaction(
                    accountId = defaultAccountId,
                    categoryId = currentState.categoryId,
                    type = currentState.type,
                    amount = amountValue,
                    currency = "CNY",
                    title = autoTitle,
                    description = currentState.note.takeIf { it.isNotBlank() },
                    date = currentState.date,
                    sourceType = TransactionSourceType.MANUAL,
                    userConfirmed = true
                )

                transactionRepository.insertTransaction(transaction)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    amountError = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AddTransactionUiState(
            categoryId = CategoryCatalog.fallback(TransactionType.EXPENSE).id,
        )
    }
}

private fun mergeCategories(type: TransactionType, customCategories: List<Category>): List<Category> {
    val defaults = CategoryCatalog.categoriesForType(type)
    val customForType = customCategories.filter { it.type == type && !it.isDefault }
    return defaults + customForType
}

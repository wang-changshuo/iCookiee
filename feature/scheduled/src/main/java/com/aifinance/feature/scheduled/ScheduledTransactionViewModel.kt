package com.aifinance.feature.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.ScheduledRuleRepository
import com.aifinance.core.data.schedule.ScheduleOccurrenceCalculator
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class ScheduledFormState(
    val title: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categoryId: UUID? = null,
    val accountId: UUID? = null,
    val amount: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val recurrence: ScheduledRecurrence = ScheduledRecurrence.DAILY,
    val endMode: ScheduledEndMode = ScheduledEndMode.NEVER,
    val endDate: LocalDate = LocalDate.now().plusMonths(1),
    val maxOccurrences: String = "10",
    val saveError: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class ScheduledTransactionViewModel @Inject constructor(
    private val scheduledRuleRepository: ScheduledRuleRepository,
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val scheduledRuleScheduler: ScheduledRuleScheduler,
) : ViewModel() {

    private val _form = MutableStateFlow(
        ScheduledFormState(
            categoryId = CategoryCatalog.fallback(TransactionType.EXPENSE).id,
        ),
    )
    val form: StateFlow<ScheduledFormState> = _form.asStateFlow()

    val rules: StateFlow<List<ScheduledRule>> = scheduledRuleRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts = accountRepository.getActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = combine(
        _form,
        categoryRepository.getAllCategories(),
    ) { state, custom ->
        mergeCategories(state.transactionType, custom)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryCatalog.categoriesForType(TransactionType.EXPENSE),
    )

    init {
        viewModelScope.launch {
            val id = accountRepository.getDefaultIncomeExpenseAccount()?.id
                ?: accountRepository.getFirstActiveAccount()?.id
            if (id != null) {
                _form.update { it.copy(accountId = id) }
            }
        }
    }

    fun updateForm(transform: (ScheduledFormState) -> ScheduledFormState) {
        _form.update(transform)
    }

    fun onTypeChanged(type: TransactionType) {
        if (type == TransactionType.TRANSFER) return
        val nextCategory = _form.value.categoryId
            ?.takeIf { selectedId -> CategoryCatalog.resolve(selectedId, type).id == selectedId }
            ?: CategoryCatalog.fallback(type).id
        _form.update {
            it.copy(transactionType = type, categoryId = nextCategory)
        }
    }

    fun saveRule() {
        val state = _form.value
        var err: String? = null

        val amountValue = try {
            BigDecimal(state.amount.ifBlank { "0" })
        } catch (_: NumberFormatException) {
            BigDecimal.ZERO
        }
        if (amountValue <= BigDecimal.ZERO) err = "请输入大于 0 的金额"
        if (state.categoryId == null) err = "请选择分类"
        if (state.accountId == null) err = "请选择账户"

        if (state.endMode == ScheduledEndMode.AFTER_COUNT) {
            val n = state.maxOccurrences.toIntOrNull()
            if (n == null || n <= 0) err = "请输入有效的执行次数"
        }
        if (state.endMode == ScheduledEndMode.END_DATE) {
            if (state.endDate.isBefore(state.startDate)) err = "结束日期不能早于开始日期"
        }

        if (err != null) {
            _form.update { it.copy(saveError = err) }
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, saveError = null) }
            try {
                val account = accounts.value.find { it.id == state.accountId }!!
                val rawStart = LocalDateTime.of(
                    state.startDate,
                    LocalTime.of(state.startHour, state.startMinute),
                )
                val aligned = ScheduleOccurrenceCalculator.alignStartToRecurrence(
                    rawStart,
                    state.recurrence,
                )
                val rule = ScheduledRule(
                    title = state.title.ifBlank { "定时记账" },
                    transactionType = state.transactionType,
                    categoryId = state.categoryId,
                    accountId = state.accountId!!,
                    amount = amountValue,
                    currency = account.currency,
                    startDate = aligned.toLocalDate(),
                    startHour = aligned.hour,
                    startMinute = aligned.minute,
                    recurrence = state.recurrence,
                    endMode = state.endMode,
                    endDate = if (state.endMode == ScheduledEndMode.END_DATE) state.endDate else null,
                    maxOccurrences = if (state.endMode == ScheduledEndMode.AFTER_COUNT) {
                        state.maxOccurrences.toIntOrNull()
                    } else {
                        null
                    },
                )
                scheduledRuleRepository.insert(rule)
                scheduledRuleScheduler.scheduleRule(rule.id)
                _form.value = ScheduledFormState(
                    categoryId = CategoryCatalog.fallback(state.transactionType).id,
                    accountId = state.accountId,
                    transactionType = state.transactionType,
                )
            } catch (e: Exception) {
                _form.update {
                    it.copy(saveError = e.message ?: "保存失败", isSaving = false)
                }
                return@launch
            }
            _form.update { it.copy(isSaving = false, saveError = null) }
        }
    }

    fun setRuleEnabled(rule: ScheduledRule, enabled: Boolean) {
        viewModelScope.launch {
            val updated = rule.copy(enabled = enabled, updatedAt = Instant.now())
            scheduledRuleRepository.update(updated)
            if (enabled) {
                scheduledRuleScheduler.scheduleRule(rule.id)
            } else {
                scheduledRuleScheduler.cancelRule(rule.id)
            }
        }
    }

    fun deleteRule(id: UUID) {
        viewModelScope.launch {
            scheduledRuleScheduler.cancelRule(id)
            scheduledRuleRepository.delete(id)
        }
    }
}

private fun mergeCategories(type: TransactionType, customCategories: List<Category>): List<Category> {
    val defaults = CategoryCatalog.categoriesForType(type)
    val customForType = customCategories.filter { it.type == type && !it.isDefault }
    return defaults + customForType
}

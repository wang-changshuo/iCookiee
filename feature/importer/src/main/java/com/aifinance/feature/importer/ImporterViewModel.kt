package com.aifinance.feature.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

enum class ImportChannel(val label: String) {
    WECHAT("微信账单"),
    ALIPAY("支付宝账单"),
    BANK("银行账单"),
}

data class ImporterUiState(
    val selectedChannel: ImportChannel = ImportChannel.BANK,
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val lastBatchId: UUID? = null,
    val message: String? = null,
)

@HiltViewModel
class ImporterViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImporterUiState())
    val uiState: StateFlow<ImporterUiState> = _uiState.asStateFlow()

    fun selectChannel(channel: ImportChannel) {
        _uiState.value = _uiState.value.copy(selectedChannel = channel, message = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun importBankStatement(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, message = null)
            runCatching {
                val rows = BankStatementParser.parse(context, uri)
                val accountId = accountRepository.getDefaultIncomeExpenseAccount()?.id
                    ?: accountRepository.getFirstActiveAccount()?.id
                    ?: error("未找到可用账户，请先创建账户")
                val batchId = UUID.randomUUID()
                val zoneId = ZoneId.systemDefault()

                var imported = 0
                rows.forEach { row ->
                    val categoryId = if (row.type == TransactionType.INCOME) {
                        CategoryCatalog.Ids.IncomeCareer
                    } else {
                        CategoryCatalog.Ids.ExpenseFood
                    }
                    val transaction = Transaction(
                        accountId = accountId,
                        categoryId = categoryId,
                        type = row.type,
                        amount = row.amount,
                        currency = "CNY",
                        title = row.title,
                        description = row.note,
                        date = row.dateTime.toLocalDate(),
                        time = row.dateTime.atZone(zoneId).toInstant(),
                        sourceType = TransactionSourceType.IMPORTED_BANK,
                        importBatchId = batchId,
                        userConfirmed = true,
                    )
                    transactionRepository.insertTransaction(transaction)
                    imported++
                }
                batchId to imported
            }.onSuccess { (batchId, imported) ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importedCount = imported,
                    lastBatchId = batchId,
                    message = if (imported > 0) "导入成功：共 $imported 条银行账单" else "未识别到可导入的记录",
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "导入失败：${throwable.message ?: "未知错误"}",
                )
            }
        }
    }
}

data class ParsedBankBill(
    val dateTime: java.time.LocalDateTime,
    val amount: BigDecimal,
    val type: TransactionType,
    val title: String,
    val note: String?,
)

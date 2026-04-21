package com.aifinance.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.repository.ai.AIRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun onSuggestionClick(question: String) {
        _uiState.value = _uiState.value.copy(inputText = question)
        sendMessage()
    }

    fun rotateSuggestionGroup(groupCount: Int) {
        if (groupCount <= 0) return
        val nextIndex = (_uiState.value.suggestionGroupIndex + 1) % groupCount
        _uiState.value = _uiState.value.copy(suggestionGroupIndex = nextIndex)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = AssistantMessage(role = AssistantRole.USER, content = text)
        _uiState.value = _uiState.value.copy(
            inputText = "",
            messages = _uiState.value.messages + userMessage,
            isLoading = true
        )

        viewModelScope.launch {
            val systemContext = buildSystemContext()
            aiRepository.sendMessageWithContext(text, systemContext)
                .onSuccess { response ->
                    val processedResponse = processAIResponse(response)
                    val assistantMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = processedResponse
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    val errorMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = "抱歉，发生了错误：${error.message}"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + errorMessage,
                        isLoading = false
                    )
                }
        }
    }

    fun sendImageForOCR(imageUri: Uri, file: File) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            aiRepository.recognizeImage(file)
                .onSuccess { ocrText ->
                    val prompt = "我上传了一张账单图片，OCR识别结果如下：\n\n$ocrText\n\n请帮我提取关键信息（金额、日期、商家、分类等），并建议如何记录这笔交易。"
                    aiRepository.sendMessage(prompt)
                        .onSuccess { response ->
                            val assistantMessage = AssistantMessage(
                                role = AssistantRole.ASSISTANT,
                                content = response
                            )
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + assistantMessage,
                                isLoading = false
                            )
                        }
                        .onFailure { error ->
                            val errorMessage = AssistantMessage(
                                role = AssistantRole.ASSISTANT,
                                content = "OCR识别成功，但AI分析失败：${error.message}"
                            )
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + errorMessage,
                                isLoading = false
                            )
                        }
                }
                .onFailure { error ->
                    val errorMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = "OCR识别失败：${error.message}"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + errorMessage,
                        isLoading = false
                    )
                }
        }
    }

    fun clearConversation() {
        aiRepository.clearConversation()
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            inputText = ""
        )
    }

    private suspend fun buildSystemContext(): String {
        val accounts = accountRepository.getActiveAccounts().first()
        val categories = categoryRepository.getAllCategories().first()
        val allTransactions = transactionRepository.getAllTransactions().first()

        val now = LocalDate.now()
        val yesterday = now.minusDays(1)
        val sevenDaysAgo = now.minusDays(6)
        val thirtyDaysAgo = now.minusDays(29)
        val currentMonth = now.monthValue
        val currentYear = now.year

        val dayOfWeek = now.dayOfWeek.value
        val weekStart = now.minusDays((dayOfWeek - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        val lastWeekStart = weekStart.minusDays(7)
        val lastWeekEnd = weekStart.minusDays(1)

        val monthStart = now.withDayOfMonth(1)
        val lastMonth = now.minusMonths(1)
        val lastMonthStart = lastMonth.withDayOfMonth(1)
        val lastMonthEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())

        fun calculateStats(transactions: List<Transaction>): Triple<BigDecimal, BigDecimal, Int> {
            val income = transactions.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
            return Triple(income, expense, transactions.size)
        }

        fun filterByDateRange(start: LocalDate, end: LocalDate) =
            allTransactions.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }

        val (todayIncome, todayExpense, todayCount) = calculateStats(
            allTransactions.filter { it.date == now }
        )
        val (yesterdayIncome, yesterdayExpense, yesterdayCount) = calculateStats(
            allTransactions.filter { it.date == yesterday }
        )
        val (weekIncome, weekExpense, weekCount) = calculateStats(
            filterByDateRange(weekStart, weekEnd)
        )
        val (lastWeekIncome, lastWeekExpense, lastWeekCount) = calculateStats(
            filterByDateRange(lastWeekStart, lastWeekEnd)
        )
        val (sevenDaysIncome, sevenDaysExpense, sevenDaysCount) = calculateStats(
            filterByDateRange(sevenDaysAgo, now)
        )
        val (monthIncome, monthExpense, monthCount) = calculateStats(
            filterByDateRange(monthStart, now)
        )
        val (lastMonthIncome, lastMonthExpense, lastMonthCount) = calculateStats(
            filterByDateRange(lastMonthStart, lastMonthEnd)
        )
        val (thirtyDaysIncome, thirtyDaysExpense, thirtyDaysCount) = calculateStats(
            filterByDateRange(thirtyDaysAgo, now)
        )

        val daysInMonth = now.lengthOfMonth()
        val avgDailyExpense = if (daysInMonth > 0) {
            monthExpense.divide(BigDecimal(daysInMonth), 2, java.math.RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val accountInfo = accounts.joinToString("\n") { acc ->
            "  - ${acc.name} (${acc.type}): ¥${acc.currentBalance}"
        }

        val defaultCategories = CategoryCatalog.all.joinToString(", ") { it.name }
        val customCategories = categories.filter { !it.isDefault }.joinToString(", ") { it.name }

        val allCategoryIds = CategoryCatalog.all.map { it.id } + categories.map { it.id }
        val categoryStats = allCategoryIds.mapNotNull { catId ->
            val catTransactions = allTransactions.filter { it.categoryId == catId }
            if (catTransactions.isEmpty()) null
            else {
                val catIncome = catTransactions.filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                val catExpense = catTransactions.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                val catName = CategoryCatalog.all.find { it.id == catId }?.name
                    ?: categories.find { it.id == catId }?.name
                    ?: "未知分类"
                Triple(catName, catIncome, catExpense)
            }
        }.sortedByDescending { it.second + it.third }

        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        val todayWeekDay = weekDays[now.dayOfWeek.value - 1]
        val yesterdayWeekDay = weekDays[yesterday.dayOfWeek.value - 1]

        return """
你是iCookie智能记账助手的AI核心，深度集成在用户的记账App中。你可以访问用户的实时财务数据。

【当前时间】
今天是 ${now.year}年${now.monthValue}月${now.dayOfMonth}日，星期$todayWeekDay
昨天是 ${yesterday.year}年${yesterday.monthValue}月${yesterday.dayOfMonth}日，星期$yesterdayWeekDay

【数据新鲜度说明】
以下所有财务数据均为实时查询的最新数据（截至当前时间）。
请优先使用本次提供的最新数据回答用户问题。
如果数据与之前对话有变化，请以本次最新数据为准。

【时间范围对照表】
- "今天" = ${now.year}年${now.monthValue}月${now.dayOfMonth}日
- "昨天" = ${yesterday.year}年${yesterday.monthValue}月${yesterday.dayOfMonth}日
- "本周" = ${weekStart.monthValue}月${weekStart.dayOfMonth}日 至 ${weekEnd.monthValue}月${weekEnd.dayOfMonth}日
- "上周" = ${lastWeekStart.monthValue}月${lastWeekStart.dayOfMonth}日 至 ${lastWeekEnd.monthValue}月${lastWeekEnd.dayOfMonth}日
- "最近7天" = ${sevenDaysAgo.monthValue}月${sevenDaysAgo.dayOfMonth}日 至 ${now.monthValue}月${now.dayOfMonth}日
- "最近30天" = ${thirtyDaysAgo.monthValue}月${thirtyDaysAgo.dayOfMonth}日 至 ${now.monthValue}月${now.dayOfMonth}日
- "本月" = ${currentYear}年${currentMonth}月 (${monthStart.monthValue}月${monthStart.dayOfMonth}日至今)
- "上月" = ${lastMonth.year}年${lastMonth.monthValue}月 (${lastMonthStart.monthValue}月${lastMonthStart.dayOfMonth}日至${lastMonthEnd.monthValue}月${lastMonthEnd.dayOfMonth}日)

【今日数据】(${now.monthValue}月${now.dayOfMonth}日)
收入：¥$todayIncome | 支出：¥$todayExpense | 笔数：${todayCount}笔

【昨日数据】(${yesterday.monthValue}月${yesterday.dayOfMonth}日)
收入：¥$yesterdayIncome | 支出：¥$yesterdayExpense | 笔数：${yesterdayCount}笔

【本周数据】(本周一至周日)
收入：¥$weekIncome | 支出：¥$weekExpense | 笔数：${weekCount}笔

【上周数据】(上周一至周日)
收入：¥$lastWeekIncome | 支出：¥$lastWeekExpense | 笔数：${lastWeekCount}笔

【最近7天数据】
收入：¥$sevenDaysIncome | 支出：¥$sevenDaysExpense | 笔数：${sevenDaysCount}笔

【本月数据】(${currentYear}年${currentMonth}月)
收入：¥$monthIncome | 支出：¥$monthExpense | 笔数：${monthCount}笔 | 日均支出：¥$avgDailyExpense

【上月数据】(${lastMonth.year}年${lastMonth.monthValue}月)
收入：¥$lastMonthIncome | 支出：¥$lastMonthExpense | 笔数：${lastMonthCount}笔

【最近30天数据】
收入：¥$thirtyDaysIncome | 支出：¥$thirtyDaysExpense | 笔数：${thirtyDaysCount}笔

【用户资产账户】
$accountInfo

【各分类交易统计】(按支出金额排序)
${categoryStats.joinToString("\n") { (name, income, expense) ->
    "  - $name: 收入¥$income | 支出¥$expense"
}}

【最近20笔交易详情】
${allTransactions.take(20).map { t ->
    val catName = CategoryCatalog.all.find { it.id == t.categoryId }?.name
        ?: categories.find { it.id == t.categoryId }?.name
        ?: "未分类"
    val typeStr = if (t.type == TransactionType.INCOME) "收入" else "支出"
    val accName = accounts.find { it.id == t.accountId }?.name ?: "未知账户"
    val timeStr = java.time.LocalDateTime.ofInstant(t.time, java.time.ZoneId.of("Asia/Shanghai"))
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val titleStr = t.title.takeIf { it != catName }?.let { "($it)" } ?: ""
    val descStr = t.description?.let { " | 备注:$it" } ?: ""
    "  - ${t.date} $timeStr [$catName]$titleStr: ${typeStr}¥${t.amount} (${accName})$descStr"
}.joinToString("\n")}

【可用分类】
默认分类：$defaultCategories
自定义分类：$customCategories

【记账方法】
当用户想记账时，必须提取完整的时间信息，使用以下格式返回交易数据：
```create_transaction
{
  "amount": 35.0,
  "type": "EXPENSE",
  "category": "餐饮",
  "title": "午餐",
  "description": "公司附近餐厅",
  "accountId": "账户UUID(可选)",
  "date": "2026-03-30",
  "time": "17:50"
}
```
注意：
- date字段使用ISO格式：yyyy-MM-dd
- time字段使用24小时制：HH:mm
- 如果用户说了具体时间（如"下午5点半"），必须转换为time格式
- 如果用户说"今天"，date使用今天的日期
- 如果用户说"昨天"，date使用昨天的日期

【回答规则】
1. 用户问"今天"，使用今日数据
2. 用户问"昨天"，使用昨日数据
3. 用户问"本周"，使用本周数据
4. 用户问"上周"，使用上周数据
5. 用户问"最近7天/这周"，使用最近7天数据
6. 用户问"最近30天/这个月"，使用最近30天数据
7. 用户问"本月"，使用本月数据
8. 用户问"上月"，使用上月数据
9. 优先使用上述统计数据，不要自己计算
        """.trimIndent()
    }

    private suspend fun processAIResponse(response: String): String {
        val createTransactionPattern = "```create_transaction\\s*\\n(.*?)\\n```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = createTransactionPattern.find(response)

        if (match != null) {
            val jsonContent = match.groupValues[1]
            return try {
                val transactionData = parseTransactionJson(jsonContent)
                createTransactionFromAI(transactionData)
                "✅ 已成功记录交易！\n\n" + response.replace(match.value, "")
            } catch (e: Exception) {
                "❌ 创建交易失败：${e.message}\n\n" + response.replace(match.value, "")
            }
        }

        return response
    }

    private data class AITransactionData(
        val amount: Double,
        val type: String,
        val category: String?,
        val title: String?,
        val description: String?,
        val accountId: String?,
        val date: String?,
        val time: String?
    )

    private fun parseTransactionJson(json: String): AITransactionData {
        val amountPattern = """"amount"\s*:\s*([\d.]+)""".toRegex()
        val typePattern = """"type"\s*:\s*"([^"]+)"""".toRegex()
        val categoryPattern = """"category"\s*:\s*"([^"]*)"""".toRegex()
        val titlePattern = """"title"\s*:\s*"([^"]*)"""".toRegex()
        val descriptionPattern = """"description"\s*:\s*"([^"]*)"""".toRegex()
        val accountIdPattern = """"accountId"\s*:\s*"([^"]*)"""".toRegex()
        val datePattern = """"date"\s*:\s*"([^"]*)"""".toRegex()
        val timePattern = """"time"\s*:\s*"([^"]*)"""".toRegex()

        return AITransactionData(
            amount = amountPattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: throw IllegalArgumentException("金额无效"),
            type = typePattern.find(json)?.groupValues?.get(1) ?: "EXPENSE",
            category = categoryPattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() },
            title = titlePattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() },
            description = descriptionPattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() },
            accountId = accountIdPattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() },
            date = datePattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() },
            time = timePattern.find(json)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
        )
    }

    private suspend fun createTransactionFromAI(data: AITransactionData) {
        val accounts = accountRepository.getActiveAccounts().first()
        val targetAccount = data.accountId?.let { uuid ->
            accounts.find { it.id.toString() == uuid }
        } ?: accounts.firstOrNull { it.isDefaultIncomeExpense } ?: accounts.firstOrNull()
        ?: throw IllegalStateException("没有可用账户")

        val transactionType = try {
            TransactionType.valueOf(data.type.uppercase())
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }

        val categories = categoryRepository.getAllCategories().first()
        val categoryId = data.category?.let { catName ->
            CategoryCatalog.forType(transactionType)
                .firstOrNull { it.name == catName }?.id
                ?: categories.firstOrNull { it.name == catName && it.type == transactionType }?.id
        } ?: CategoryCatalog.forType(transactionType).firstOrNull()?.id

        val transactionDate = data.date?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                LocalDate.now()
            }
        } ?: LocalDate.now()

        val transactionTime = data.time?.let {
            try {
                val localTime = java.time.LocalTime.parse(it)
                AppDateTime.toInstant(LocalDateTime.of(transactionDate, localTime))
            } catch (e: Exception) {
                AppDateTime.toInstant(LocalDateTime.now())
            }
        } ?: AppDateTime.toInstant(LocalDateTime.now())

        val transaction = Transaction(
            id = UUID.randomUUID(),
            accountId = targetAccount.id,
            categoryId = categoryId,
            type = transactionType,
            amount = BigDecimal.valueOf(data.amount),
            currency = "CNY",
            title = data.title ?: data.category ?: "未分类",
            description = data.description,
            date = transactionDate,
            time = transactionTime,
            sourceType = TransactionSourceType.MANUAL,
            isPending = false
        )

        transactionRepository.insertTransaction(transaction)
    }
}

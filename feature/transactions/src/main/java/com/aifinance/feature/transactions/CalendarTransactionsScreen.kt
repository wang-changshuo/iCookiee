package com.aifinance.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.component.RefinedTransactionItem
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTransactionsScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val monthTransactions by remember(transactions, currentMonth) {
        derivedStateOf {
            transactions.filter {
                it.date.year == currentMonth.year &&
                        it.date.monthValue == currentMonth.monthValue &&
                        !it.isPending
            }
        }
    }

    val dayMap by remember(monthTransactions) {
        derivedStateOf { monthTransactions.groupBy { it.date } }
    }

    val selectedDayTransactions by remember(dayMap, selectedDate) {
        derivedStateOf {
            dayMap[selectedDate].orEmpty().sortedByDescending { it.time }
        }
    }

    val categoriesById by remember(viewModel.categories) {
        derivedStateOf { viewModel.categories.associateBy { it.id } }
    }

    val accountsById by remember(accounts) {
        derivedStateOf { accounts.associateBy { it.id } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSelector(
                    currentMonth = currentMonth,
                    onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) },
                    onToday = {
                        val today = LocalDate.now()
                        selectedDate = today
                        currentMonth = YearMonth.from(today)
                    }
                )
            }

            item {
                WeekdayHeader()
            }

            item {
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    dayMap = dayMap,
                )
            }

            item {
                SelectedDayHeader(
                    date = selectedDate,
                    transactions = selectedDayTransactions,
                )
            }

            if (selectedDayTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("当日暂无交易", color = Color(0xFF9CA3AF))
                    }
                }
            } else {
                items(selectedDayTransactions, key = { it.id }) { transaction ->
                    val category = categoriesById[transaction.categoryId]
                        ?: CategoryCatalog.resolve(
                            categoryId = transaction.categoryId,
                            type = transaction.type
                        ).asCategory()

                    RefinedTransactionItem(
                        transaction = transaction,
                        accountName = accountsById[transaction.accountId]?.name,
                        category = category,
                        onClick = { onNavigateToTransactionDetail(transaction.id) },
                        onCategoryClick = { },
                        onAmountClick = { },
                        onLongPress = { transactionToDelete = transaction },
                    )
                }
            }
        }
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(tx)
                    transactionToDelete = null
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MonthSelector(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "上个月")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${currentMonth.year}年${String.format("%02d", currentMonth.monthValue)}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .clickable(onClick = onToday)
                    .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "回今天",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8B6914),
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "下个月")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weekDays.forEach { label ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFDDEBFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E5FE6),
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    dayMap: Map<LocalDate, List<Transaction>>,
) {
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val totalSlots = startOffset + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(7) { colIndex ->
                    val slotIndex = rowIndex * 7 + colIndex
                    if (slotIndex < startOffset || slotIndex >= startOffset + daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = slotIndex - startOffset + 1
                        val date = LocalDate.of(currentMonth.year, currentMonth.monthValue, day)
                        val txs = dayMap[date].orEmpty()
                        val isSelected = date == selectedDate
                        val income = txs
                            .filter { !it.isPending && it.type == TransactionType.INCOME }
                            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                        val expense = txs
                            .filter { !it.isPending && it.type == TransactionType.EXPENSE }
                            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
                        val hasData = income > BigDecimal.ZERO || expense > BigDecimal.ZERO

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.95f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFFFFD54F)
                                    else if (hasData) Color(0xFFFFFDF5)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 0.dp else 0.5.dp,
                                    color = if (isSelected || hasData) Color.Transparent else Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onDateSelected(date) }
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                color = Color(0xFF1F2937),
                            )
                            if (income > BigDecimal.ZERO) {
                                Text(
                                    text = "+${formatAmount(income)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = IncomeDefault,
                                    lineHeight = 10.sp,
                                )
                            }
                            if (expense > BigDecimal.ZERO) {
                                Text(
                                    text = "-${formatAmount(expense)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = ExpenseDefault,
                                    lineHeight = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayHeader(date: LocalDate, transactions: List<Transaction>) {
    val expense = transactions
        .filter { it.type == TransactionType.EXPENSE && !it.isPending }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
    val income = transactions
        .filter { it.type == TransactionType.INCOME && !it.isPending }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日（E）", Locale.CHINA)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (income > BigDecimal.ZERO || expense > BigDecimal.ZERO) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "支出¥${formatAmount(expense)} | 收入¥${formatAmount(income)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAmount(amount: BigDecimal): String {
    return DecimalFormat("#,##0.00").format(amount)
}

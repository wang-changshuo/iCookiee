package com.aifinance.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.model.Account
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var categoryPickerTransaction by remember { mutableStateOf<Transaction?>(null) }

    val grouped = remember(transactions) { transactions.groupBy { YearMonthKey(it.date.year, it.date.monthValue) } }
    val showScrollTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 6
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = showScrollTop) {
                IconButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .size(44.dp),
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "回到顶部")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无交易记录", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                grouped.toSortedMap(compareByDescending<YearMonthKey> { it.year }.thenByDescending { it.month })
                    .forEach { (yearMonth, monthTransactions) ->
                        stickyHeader {
                            MonthHeader(yearMonth = yearMonth)
                        }

                        monthTransactions.groupBy { it.date }
                            .toSortedMap(compareByDescending<LocalDate> { it })
                            .forEach { (date, dayTransactions) ->
                                item(key = "day-${date}") {
                                    DayHeader(
                                        date = date,
                                        dayTransactions = dayTransactions,
                                    )
                                }

                                items(
                                    items = dayTransactions.sortedByDescending { it.time },
                                    key = { it.id },
                                    contentType = { "transaction_item" },
                                ) { transaction ->
                                    val category = resolveCategory(transaction, viewModel.categories)
                                    TimelineTransactionItem(
                                        transaction = transaction,
                                        category = category,
                                        account = accounts.firstOrNull { it.id == transaction.accountId },
                                        modifier = Modifier,
                                        onCategoryClick = { categoryPickerTransaction = transaction },
                                        onClick = { onNavigateToTransactionDetail(transaction.id) },
                                    )
                                }
                            }
                    }
            }
        }
    }

    val categoryTarget = categoryPickerTransaction
    if (categoryTarget != null) {
        CategoryPickerSheet(
            categories = viewModel.categoriesForType(categoryTarget.type),
            selectedCategoryId = categoryTarget.categoryId,
            onDismiss = { categoryPickerTransaction = null },
            onSelect = { categoryId ->
                viewModel.updateTransactionCategory(categoryTarget, categoryId)
                categoryPickerTransaction = null
            },
        )
    }

}

@Composable
private fun MonthHeader(yearMonth: YearMonthKey) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 8.dp, bottom = 2.dp),
    ) {
        Text(
            text = "${yearMonth.month}月 ${yearMonth.year}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, dayTransactions: List<Transaction>) {
    val expense = dayTransactions.filter { it.type == TransactionType.EXPENSE && !it.isPending }
        .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
    val income = dayTransactions.filter { it.type == TransactionType.INCOME && !it.isPending }
        .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("M月d日（E）", Locale.CHINA)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "支出¥${formatAmount(expense)} | 收入¥${formatAmount(income)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineTransactionItem(
    transaction: Transaction,
    category: Category,
    account: Account?,
    modifier: Modifier = Modifier,
    onCategoryClick: () -> Unit,
    onClick: () -> Unit,
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> Color(0xFFB56B1D)
        TransactionType.EXPENSE -> Color(0xFF2F67DE)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = transaction.time.toLocalTimeText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val sign = when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.TRANSFER -> ""
                }
                Text(
                    text = "$sign¥${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFFE7EEFF), RoundedCornerShape(18.dp))
                        .clickable(onClick = onCategoryClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = category.name, color = Color(0xFF2F67DE), style = MaterialTheme.typography.titleSmall)
                    Text(text = category.icon)
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "改分类",
                        tint = Color(0xFF2F67DE),
                        modifier = Modifier.size(14.dp),
                    )
                }

                Text(
                    text = account?.name ?: "未选账户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val description = transaction.description
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategoryId: UUID?,
    onDismiss: () -> Unit,
    onSelect: (UUID) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "选择分类", style = MaterialTheme.typography.titleLarge)
            categories.forEach { category ->
                val selected = selectedCategoryId == category.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelect(category.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = category.icon)
                        Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (selected) {
                        Text(text = "已选", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    transaction: Transaction,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (amount: BigDecimal, accountId: UUID, date: LocalDate, type: TransactionType, includeInExpense: Boolean) -> Unit,
) {
    var amountText by remember(transaction.id) { mutableStateOf(formatAmount(transaction.amount)) }
    var selectedAccountId by remember(transaction.id) { mutableStateOf(transaction.accountId) }
    var selectedDate by remember(transaction.id) { mutableStateOf(transaction.date) }
    var selectedType by remember(transaction.id) { mutableStateOf(transaction.type) }
    var includeInExpense by remember(transaction.id) { mutableStateOf(!transaction.isPending) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "记录详情", style = MaterialTheme.typography.titleLarge)

            DetailRow(
                icon = Icons.Default.Edit,
                title = "金额",
                value = "¥$amountText",
                onClick = {},
            )
            AmountPad(
                amount = amountText,
                onAmountChanged = { amountText = it },
            )

            DetailRow(
                icon = Icons.Default.Wallet,
                title = "账户",
                value = accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "未选账户",
                onClick = {},
            )
            AccountRowSelector(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onSelect = { selectedAccountId = it },
            )

            DetailRow(
                icon = Icons.Default.CalendarToday,
                title = "日期",
                value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                onClick = { showDatePicker = true },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypeChip(
                    text = "支出",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    modifier = Modifier.weight(1f),
                )
                TypeChip(
                    text = "收入",
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "计入收支")
                Switch(checked = includeInExpense, onCheckedChange = { includeInExpense = it })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                Column {
                    Text(text = "地点（系统定位）", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "暂不支持手动修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = {
                    val parsedAmount = amountText.toBigDecimalOrNull()
                    if (parsedAmount != null && parsedAmount > BigDecimal.ZERO) {
                        onSave(parsedAmount, selectedAccountId, selectedDate, selectedType, includeInExpense)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("保存修改")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AmountPad(amount: String, onAmountChanged: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("清空", "0", "."),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .clickable {
                                when (key) {
                                    "清空" -> onAmountChanged("")
                                    "." -> if (!amount.contains(".")) onAmountChanged(if (amount.isEmpty()) "0." else "$amount.")
                                    else -> onAmountChanged("$amount$key")
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = key)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRowSelector(
    accounts: List<Account>,
    selectedAccountId: UUID,
    onSelect: (UUID) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.take(3).forEach { account ->
            val selected = account.id == selectedAccountId
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(account.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(text = account.name, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolveCategory(transaction: Transaction, categories: List<Category>): Category {
    val byId = categories.firstOrNull { it.id == transaction.categoryId }
    if (byId != null) return byId

    val fallback = CategoryCatalog.fallback(transaction.type).asCategory()
    return categories.firstOrNull { it.id == fallback.id } ?: fallback
}

private fun Instant.toLocalTimeText(): String {
    return atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatAmount(amount: BigDecimal): String {
    return DecimalFormat("#,##0.00").format(amount)
}

private data class YearMonthKey(
    val year: Int,
    val month: Int,
)

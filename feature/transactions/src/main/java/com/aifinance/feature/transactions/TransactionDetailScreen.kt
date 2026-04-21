package com.aifinance.feature.transactions

import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun TransactionDetailRoute(
    transactionIdArg: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactionId = remember(transactionIdArg) {
        runCatching { UUID.fromString(transactionIdArg) }.getOrNull()
    }
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val transaction = remember(transactionId, transactions) {
        transactionId?.let { targetId -> transactions.firstOrNull { it.id == targetId } }
    }

    when {
        transactionId == null -> {
            TransactionDetailFallbackScreen(
                title = "无法打开记录",
                message = "记录标识无效",
                onBack = onBack,
            )
        }

        transaction == null -> {
            TransactionDetailFallbackScreen(
                title = "记录不存在",
                message = "未找到对应交易，请返回重试",
                onBack = onBack,
            )
        }

        else -> {
            TransactionDetailEditorScreen(
                transaction = transaction,
                accounts = accounts,
                onBack = onBack,
                onSave = { editorValue ->
                    viewModel.updateTransactionEditor(
                        transaction = transaction,
                        amount = editorValue.amount,
                        accountId = editorValue.accountId,
                        dateTime = editorValue.dateTime,
                        type = editorValue.type,
                        remark = editorValue.remark,
                    )
                    onSaved()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailEditorScreen(
    transaction: Transaction,
    accounts: List<Account>,
    onBack: () -> Unit,
    onSave: (TransactionEditorValue) -> Unit,
) {
    var amountText by remember(transaction.id) { mutableStateOf(transaction.amount.stripTrailingZeros().toPlainString()) }
    var selectedType by remember(transaction.id) { mutableStateOf(transaction.type) }
    var selectedAccountId by remember(transaction.id) { mutableStateOf(transaction.accountId) }
    var selectedDateTime by remember(transaction.id) {
        mutableStateOf(LocalDateTime.of(transaction.date, transaction.time.toLocalTimeInZone()))
    }
    var remark by remember(transaction.id) { mutableStateOf(transaction.description.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    val canSave = amountText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录详情", style = IcokieTextStyles.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePrimary),
            )
        },
        containerColor = SurfaceSecondary,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E6A3)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "金额", style = IcokieTextStyles.titleMedium, color = OnSurfacePrimary)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                amountText = input.filter { it.isDigit() || it == '.' }
                            },
                            prefix = { Text("¥") },
                            singleLine = true,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(0.55f),
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Text(
                            text = "类型",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RefinedTypeChip(
                                text = "支出",
                                selected = selectedType == TransactionType.EXPENSE,
                                onClick = { selectedType = TransactionType.EXPENSE },
                                modifier = Modifier.weight(1f),
                            )
                            RefinedTypeChip(
                                text = "收入",
                                selected = selectedType == TransactionType.INCOME,
                                onClick = { selectedType = TransactionType.INCOME },
                                modifier = Modifier.weight(1f),
                            )
                            RefinedTypeChip(
                                text = "转账",
                                selected = selectedType == TransactionType.TRANSFER,
                                onClick = { selectedType = TransactionType.TRANSFER },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SelectionRow(
                            icon = Icons.Default.Wallet,
                            title = "账户",
                            value = selectedAccount?.name ?: "未选择账户",
                            onClick = {},
                        )
                        HorizontalDivider(color = SurfaceSecondary)
                        AccountSelectorGrid(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelect = { selectedAccountId = it },
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SelectionRow(
                            icon = Icons.Default.CalendarToday,
                            title = "日期",
                            value = selectedDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            onClick = { showDatePicker = true },
                        )
                        HorizontalDivider(color = SurfaceSecondary)
                        SelectionRow(
                            icon = Icons.Default.Schedule,
                            title = "时间",
                            value = selectedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedDateTime = selectedDateTime
                                            .withHour(hour)
                                            .withMinute(minute)
                                            .withSecond(0)
                                            .withNano(0)
                                    },
                                    selectedDateTime.hour,
                                    selectedDateTime.minute,
                                    true,
                                ).show()
                            },
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "备注",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            placeholder = { Text("请输入备注") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                    SelectionRow(
                        icon = Icons.Default.AttachFile,
                        title = "图片",
                        value = "添加凭证（即将支持）",
                        onClick = {},
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val parsedAmount = amountText.toBigDecimalOrNull() ?: return@Button
                        onSave(
                            TransactionEditorValue(
                                amount = parsedAmount,
                                accountId = selectedAccountId,
                                dateTime = selectedDateTime,
                                type = selectedType,
                                remark = remark.trim().ifBlank { null },
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        disabledContainerColor = OnSurfaceTertiary.copy(alpha = 0.3f),
                    ),
                ) {
                    Text("保存", style = IcokieTextStyles.titleMedium, color = Color.White)
                }
            }
        }
    }

    if (showDatePicker) {
        val millis = selectedDateTime
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            selectedDateTime = LocalDateTime.of(selectedDate, selectedDateTime.toLocalTime())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailFallbackScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RefinedTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) BrandPrimary.copy(alpha = 0.12f) else SurfaceSecondary,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium,
            color = if (selected) BrandPrimary else OnSurfaceSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SelectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = OnSurfaceSecondary)
            Text(text = title, style = IcokieTextStyles.bodyLarge, color = OnSurfacePrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = IcokieTextStyles.bodyLarge,
                color = OnSurfaceSecondary,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OnSurfaceTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AccountSelectorGrid(
    accounts: List<Account>,
    selectedAccountId: UUID,
    onSelect: (UUID) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        accounts.chunked(2).forEach { rowAccounts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowAccounts.forEach { account ->
                    val selected = account.id == selectedAccountId
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) BrandPrimary.copy(alpha = 0.12f) else SurfaceSecondary,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(account.id) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(account.icon)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = IcokieTextStyles.labelMedium,
                                color = OnSurfacePrimary,
                                maxLines = 1,
                            )
                            if (account.isDefaultIncomeExpense) {
                                Text(
                                    text = "默认",
                                    style = IcokieTextStyles.labelSmall,
                                    color = BrandPrimary,
                                )
                            }
                        }
                    }
                }
                if (rowAccounts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun Instant.toLocalTimeInZone(zoneId: ZoneId = AppDateTime.zoneId): java.time.LocalTime {
    return atZone(zoneId).toLocalTime()
}

private data class TransactionEditorValue(
    val amount: BigDecimal,
    val accountId: UUID,
    val dateTime: LocalDateTime,
    val type: TransactionType,
    val remark: String?,
)

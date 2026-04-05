package com.aifinance.feature.scheduled

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.aifinance.core.model.Account
import com.aifinance.core.model.Category
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val recurrenceLabels = mapOf(
    ScheduledRecurrence.DAILY to "每天",
    ScheduledRecurrence.WEEKLY to "每周",
    ScheduledRecurrence.MONTHLY to "每月",
    ScheduledRecurrence.WEEKDAYS to "工作日",
    ScheduledRecurrence.WEEKENDS to "周六日",
    ScheduledRecurrence.EVERY_THREE_MONTHS to "每三个月",
    ScheduledRecurrence.EVERY_SIX_MONTHS to "每六个月",
    ScheduledRecurrence.YEARLY to "每年",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduledTransactionScreen(
    onBack: () -> Unit,
    viewModel: ScheduledTransactionViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "定时记账",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "已有规则",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            if (rules.isEmpty()) {
                item {
                    Text(
                        "暂无定时任务，在下方新建一条。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    ScheduledRuleCard(
                        rule = rule,
                        onEnabledChange = { en -> viewModel.setRuleEnabled(rule, en) },
                        onDelete = { viewModel.deleteRule(rule.id) },
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "新建定时记账",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                Text("标题（可选）", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { viewModel.updateForm { s -> s.copy(title = it) } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("默认：定时记账") },
                    singleLine = true,
                )
            }

            item {
                Text("类型", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.transactionType == TransactionType.EXPENSE,
                        onClick = { viewModel.onTypeChanged(TransactionType.EXPENSE) },
                        label = { Text("支出") },
                    )
                    FilterChip(
                        selected = form.transactionType == TransactionType.INCOME,
                        onClick = { viewModel.onTypeChanged(TransactionType.INCOME) },
                        label = { Text("收入") },
                    )
                }
            }

            item {
                Text("分类", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = form.categoryId == cat.id,
                            onClick = { viewModel.updateForm { s -> s.copy(categoryId = cat.id) } },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }

            item {
                Text("账户", style = MaterialTheme.typography.labelMedium)
                AccountDropdown(
                    accounts = accounts,
                    selectedId = form.accountId,
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it },
                    onSelected = {
                        viewModel.updateForm { s -> s.copy(accountId = it) }
                        accountMenuExpanded = false
                    },
                )
            }

            item {
                Text("每次金额", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = { v ->
                        val filtered = v.filter { it.isDigit() || it == '.' }
                        viewModel.updateForm { s -> s.copy(amount = filtered) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00") },
                    singleLine = true,
                )
            }

            item {
                Text("开始日期与时间", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showStartDatePicker = true }) {
                        Text(form.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    viewModel.updateForm { s -> s.copy(startHour = h, startMinute = m) }
                                },
                                form.startHour,
                                form.startMinute,
                                true,
                            ).show()
                        },
                    ) {
                        Text(
                            "%02d:%02d".format(form.startHour, form.startMinute),
                        )
                    }
                }
            }

            item {
                Text("重复周期", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recurrenceMenuExpanded = true },
                        readOnly = true,
                        value = recurrenceLabels[form.recurrence] ?: form.recurrence.name,
                        onValueChange = {},
                    )
                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                    ) {
                        ScheduledRecurrence.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(recurrenceLabels[r] ?: r.name) },
                                onClick = {
                                    viewModel.updateForm { s -> s.copy(recurrence = r) }
                                    recurrenceMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            item {
                Text("结束方式", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = form.endMode == ScheduledEndMode.NEVER,
                        onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.NEVER) } },
                        label = { Text("不结束") },
                    )
                    FilterChip(
                        selected = form.endMode == ScheduledEndMode.AFTER_COUNT,
                        onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.AFTER_COUNT) } },
                        label = { Text("按次数") },
                    )
                    FilterChip(
                        selected = form.endMode == ScheduledEndMode.END_DATE,
                        onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.END_DATE) } },
                        label = { Text("按日期") },
                    )
                }
                when (form.endMode) {
                    ScheduledEndMode.AFTER_COUNT -> {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = form.maxOccurrences,
                            onValueChange = { viewModel.updateForm { s -> s.copy(maxOccurrences = it.filter { c -> c.isDigit() }) } },
                            label = { Text("执行次数后停止") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    ScheduledEndMode.END_DATE -> {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showEndDatePicker = true }) {
                            Text("结束日期：${form.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                        }
                    }
                    ScheduledEndMode.NEVER -> Unit
                }
            }

            item {
                form.saveError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.saveRule() },
                    enabled = !form.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (form.isSaving) "保存中…" else "保存定时任务", color = BrandPrimary)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerModal(
            initialDate = form.startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { millis ->
                showStartDatePicker = false
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    viewModel.updateForm { s -> s.copy(startDate = date) }
                }
            },
        )
    }
    if (showEndDatePicker) {
        DatePickerModal(
            initialDate = form.endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { millis ->
                showEndDatePicker = false
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    viewModel.updateForm { s -> s.copy(endDate = date) }
                }
            },
        )
    }
}

@Composable
private fun ScheduledRuleCard(
    rule: ScheduledRule,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.title.ifBlank { "定时记账" }, fontWeight = FontWeight.SemiBold)
                val typeText = when (rule.transactionType) {
                    TransactionType.INCOME -> "收入"
                    TransactionType.EXPENSE -> "支出"
                    else -> rule.transactionType.name
                }
                Text(
                    "$typeText · ${rule.amount} · ${recurrenceLabels[rule.recurrence] ?: rule.recurrence.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatNextRun(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandPrimary,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFEF4444))
            }
        }
    }
}

private fun formatNextRun(rule: ScheduledRule): String {
    val n = rule.nextRunAt ?: return if (rule.enabled) "排程计算中…" else "已关闭"
    val z = ZoneId.systemDefault()
    val dt = LocalDateTime.ofInstant(n, z)
    return "下次：${dt.toLocalDate()} ${"%02d:%02d".format(dt.hour, dt.minute)}"
}

@Composable
private fun AccountDropdown(
    accounts: List<Account>,
    selectedId: UUID?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (UUID) -> Unit,
) {
    val selected = accounts.find { it.id == selectedId }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
            readOnly = true,
            value = selected?.name ?: "请选择账户",
            onValueChange = {},
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    onClick = { onSelected(acc.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

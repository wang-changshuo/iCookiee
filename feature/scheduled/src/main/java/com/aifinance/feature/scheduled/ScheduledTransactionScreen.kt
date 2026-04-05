package com.aifinance.feature.scheduled

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.BorderSubtle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.ExpenseBackground
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.Category
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Display order matches product spec (bottom sheet list). */
private val recurrencePickerOrder: List<ScheduledRecurrence> = listOf(
    ScheduledRecurrence.DAILY,
    ScheduledRecurrence.WEEKDAYS,
    ScheduledRecurrence.WEEKENDS,
    ScheduledRecurrence.WEEKLY,
    ScheduledRecurrence.MONTHLY,
    ScheduledRecurrence.LAST_DAY_OF_MONTH,
    ScheduledRecurrence.EVERY_THREE_MONTHS,
    ScheduledRecurrence.EVERY_SIX_MONTHS,
    ScheduledRecurrence.YEARLY,
)

private fun recurrenceLabel(r: ScheduledRecurrence): String = when (r) {
    ScheduledRecurrence.DAILY -> "每天"
    ScheduledRecurrence.WEEKDAYS -> "每个工作日"
    ScheduledRecurrence.WEEKENDS -> "每个周六日"
    ScheduledRecurrence.WEEKLY -> "每周"
    ScheduledRecurrence.MONTHLY -> "每月"
    ScheduledRecurrence.LAST_DAY_OF_MONTH -> "每月最后一天"
    ScheduledRecurrence.EVERY_THREE_MONTHS -> "每3个月"
    ScheduledRecurrence.EVERY_SIX_MONTHS -> "每6个月"
    ScheduledRecurrence.YEARLY -> "每年"
}

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
    val scope = rememberCoroutineScope()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showRecurrenceSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    val recurrenceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = SurfacePrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "定时记账",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OnSurfaceSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePrimary,
                    titleContentColor = OnSurfacePrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "已有规则",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                if (rules.isEmpty()) {
                    item {
                        Text(
                            "暂无定时任务，在下方新建一条。",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfaceTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp),
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
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "新建定时记账",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                "标题（可选）",
                                style = IcokieTextStyles.labelMedium,
                                color = OnSurfaceSecondary,
                            )
                            OutlinedTextField(
                                value = form.title,
                                onValueChange = { viewModel.updateForm { s -> s.copy(title = it) } },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("默认：定时记账", color = OnSurfaceTertiary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceSecondary,
                                    unfocusedContainerColor = SurfaceSecondary,
                                ),
                            )

                            Text(
                                "类型",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ScheduledTypeButton(
                                    text = "支出",
                                    selected = form.transactionType == TransactionType.EXPENSE,
                                    onClick = { viewModel.onTypeChanged(TransactionType.EXPENSE) },
                                    selectedColor = ExpenseDefault,
                                    modifier = Modifier.weight(1f),
                                )
                                ScheduledTypeButton(
                                    text = "收入",
                                    selected = form.transactionType == TransactionType.INCOME,
                                    onClick = { viewModel.onTypeChanged(TransactionType.INCOME) },
                                    selectedColor = IncomeDefault,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Text(
                                text = "选择分类",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                            ) {
                                items(categories, key = { it.id }) { cat ->
                                    ScheduledCategoryIconWithLabel(
                                        icon = cat.icon,
                                        label = cat.name,
                                        backgroundColor = Color(cat.color),
                                        selected = form.categoryId == cat.id,
                                        onClick = { viewModel.updateForm { s -> s.copy(categoryId = cat.id) } },
                                    )
                                }
                            }

                            Text(
                                "账户",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            val selectedAccount = accounts.find { it.id == form.accountId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ScheduledInfoChip(
                                    icon = "\uD83D\uDCB0",
                                    text = selectedAccount?.name ?: "选择账户",
                                    onClick = { showAccountSheet = true },
                                )
                                ScheduledInfoChip(
                                    icon = "\uD83D\uDCC5",
                                    text = form.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                    onClick = { showStartDatePicker = true },
                                )
                                ScheduledInfoChip(
                                    icon = "\u23F0",
                                    text = "%02d:%02d".format(form.startHour, form.startMinute),
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
                                )
                            }

                            Text(
                                "每次金额",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text = "\u00A5",
                                    style = TextStyle(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (form.transactionType) {
                                            TransactionType.EXPENSE -> ExpenseDefault
                                            TransactionType.INCOME -> IncomeDefault
                                            else -> BrandPrimary
                                        },
                                    ),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                BasicTextField(
                                    value = form.amount,
                                    onValueChange = { v ->
                                        val filtered = v.filter { it.isDigit() || it == '.' }
                                        if (filtered.length <= 12) {
                                            viewModel.updateForm { s -> s.copy(amount = filtered) }
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (form.transactionType) {
                                            TransactionType.EXPENSE -> ExpenseDefault
                                            TransactionType.INCOME -> IncomeDefault
                                            else -> BrandPrimary
                                        },
                                        textAlign = TextAlign.Start,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done,
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (form.amount.isEmpty()) {
                                            Text(
                                                "0.00",
                                                style = TextStyle(
                                                    fontSize = 40.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSurfaceTertiary,
                                                ),
                                            )
                                        }
                                        inner()
                                    },
                                )
                            }

                            Text(
                                "重复周期",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceSecondary)
                                    .clickable { showRecurrenceSheet = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    recurrenceLabel(form.recurrence),
                                    style = IcokieTextStyles.bodyLarge,
                                    color = OnSurfacePrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = OnSurfaceTertiary,
                                )
                            }

                            Text(
                                "结束方式",
                                style = IcokieTextStyles.bodyLarge,
                                color = OnSurfacePrimary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                EndModePill(
                                    text = "不结束",
                                    selected = form.endMode == ScheduledEndMode.NEVER,
                                    onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.NEVER) } },
                                )
                                EndModePill(
                                    text = "按次数",
                                    selected = form.endMode == ScheduledEndMode.AFTER_COUNT,
                                    onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.AFTER_COUNT) } },
                                )
                                EndModePill(
                                    text = "按日期",
                                    selected = form.endMode == ScheduledEndMode.END_DATE,
                                    onClick = { viewModel.updateForm { s -> s.copy(endMode = ScheduledEndMode.END_DATE) } },
                                )
                            }
                            when (form.endMode) {
                                ScheduledEndMode.AFTER_COUNT -> {
                                    OutlinedTextField(
                                        value = form.maxOccurrences,
                                        onValueChange = {
                                            viewModel.updateForm { s -> s.copy(maxOccurrences = it.filter { c -> c.isDigit() }) }
                                        },
                                        label = { Text("执行次数后停止") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandPrimary,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedContainerColor = SurfaceSecondary,
                                            unfocusedContainerColor = SurfaceSecondary,
                                        ),
                                    )
                                }
                                ScheduledEndMode.END_DATE -> {
                                    TextButton(onClick = { showEndDatePicker = true }) {
                                        Text(
                                            "结束日期：${form.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                                            color = BrandPrimary,
                                            style = IcokieTextStyles.labelMedium,
                                        )
                                    }
                                }
                                ScheduledEndMode.NEVER -> Unit
                            }

                            form.saveError?.let { err ->
                                Text(err, color = Color(0xFFDC2626), style = IcokieTextStyles.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.saveRule() },
                                enabled = !form.isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary,
                                    disabledContainerColor = BrandPrimary.copy(alpha = 0.4f),
                                ),
                            ) {
                                Text(
                                    if (form.isSaving) "保存中…" else "保存定时任务",
                                    color = OnPrimary,
                                    style = IcokieTextStyles.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showRecurrenceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecurrenceSheet = false },
            sheetState = recurrenceSheetState,
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    "重复周期",
                    style = IcokieTextStyles.titleMedium,
                    color = OnSurfacePrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
                recurrencePickerOrder.forEachIndexed { index, r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateForm { s -> s.copy(recurrence = r) }
                                scope.launch {
                                    recurrenceSheetState.hide()
                                    showRecurrenceSheet = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            recurrenceLabel(r),
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary,
                            modifier = Modifier.weight(1f),
                        )
                        RadioButton(
                            selected = form.recurrence == r,
                            onClick = {
                                viewModel.updateForm { s -> s.copy(recurrence = r) }
                                scope.launch {
                                    recurrenceSheetState.hide()
                                    showRecurrenceSheet = false
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BrandPrimary,
                                unselectedColor = OnSurfaceTertiary,
                            ),
                        )
                    }
                    if (index < recurrencePickerOrder.lastIndex) {
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = accountSheetState,
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    "选择账户",
                    style = IcokieTextStyles.titleMedium,
                    color = OnSurfacePrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                accounts.forEachIndexed { index, acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateForm { s -> s.copy(accountId = acc.id) }
                                scope.launch {
                                    accountSheetState.hide()
                                    showAccountSheet = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            acc.name,
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary,
                            modifier = Modifier.weight(1f),
                        )
                        RadioButton(
                            selected = form.accountId == acc.id,
                            onClick = {
                                viewModel.updateForm { s -> s.copy(accountId = acc.id) }
                                scope.launch {
                                    accountSheetState.hide()
                                    showAccountSheet = false
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BrandPrimary,
                                unselectedColor = OnSurfaceTertiary,
                            ),
                        )
                    }
                    if (index < accounts.lastIndex) {
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
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
private fun ScheduledTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) selectedColor.copy(alpha = 0.1f) else SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium,
            color = if (selected) selectedColor else OnSurfaceSecondary,
        )
    }
}

@Composable
private fun ScheduledCategoryIconWithLabel(
    icon: String,
    label: String,
    backgroundColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) BrandPrimary else backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, fontSize = 24.sp)
        }
        Text(
            text = label,
            style = IcokieTextStyles.labelSmall,
            color = if (selected) BrandPrimary else OnSurfaceSecondary,
        )
    }
}

@Composable
private fun ScheduledInfoChip(
    icon: String,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = text,
            style = IcokieTextStyles.labelSmall,
            color = OnSurfaceSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun EndModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) BrandPrimary.copy(alpha = 0.12f) else SurfaceSecondary,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium,
            color = if (selected) BrandPrimary else OnSurfaceSecondary,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.title.ifBlank { "定时记账" },
                    style = IcokieTextStyles.titleMedium,
                    color = OnSurfacePrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                val typeText = when (rule.transactionType) {
                    TransactionType.INCOME -> "收入"
                    TransactionType.EXPENSE -> "支出"
                    else -> rule.transactionType.name
                }
                Text(
                    "$typeText · ${rule.amount} · ${recurrenceLabel(rule.recurrence)}",
                    style = IcokieTextStyles.labelSmall,
                    color = OnSurfaceSecondary,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    formatNextRun(rule),
                    style = IcokieTextStyles.labelSmall,
                    color = BrandPrimary,
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = BrandPrimary,
                    uncheckedThumbColor = OnSurfaceTertiary,
                    uncheckedTrackColor = SurfaceSecondary,
                ),
            )
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
                Text("确定", color = BrandPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = OnSurfaceSecondary)
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

package com.aifinance.feature.scheduled

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.AppDateTimePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduledTransactionAddScreen(
    onBack: () -> Unit,
    viewModel: ScheduledTransactionViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.resetFormForAdd()
    }

    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showStartDateTimePicker by remember { mutableStateOf(false) }
    var showEndDateTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    val recurrenceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val startDateTimeLabel = remember(form.startDate, form.startHour, form.startMinute) {
        val d = form.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val t = "%02d:%02d".format(form.startHour, form.startMinute)
        "$d  $t"
    }

    Scaffold(
        containerColor = SurfacePrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "新建定时记账",
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
                                "账户与开始时间",
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
                                    text = startDateTimeLabel,
                                    onClick = { showStartDateTimePicker = true },
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
                                    TextButton(onClick = { showEndDateTimePicker = true }) {
                                        Text(
                                            "结束日期：${formatChineseDate(form.endDate)}",
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
                                onClick = {
                                    viewModel.saveRule(onSuccess = onBack)
                                },
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

    if (showStartDateTimePicker) {
        AppDateTimePickerDialog(
            initialDateTime = LocalDateTime.of(
                form.startDate,
                LocalTime.of(form.startHour, form.startMinute),
            ),
            title = "选择日期",
            onDismiss = { showStartDateTimePicker = false },
            onConfirm = { dt ->
                showStartDateTimePicker = false
                viewModel.updateForm { s ->
                    s.copy(
                        startDate = dt.toLocalDate(),
                        startHour = dt.hour,
                        startMinute = dt.minute,
                    )
                }
            },
        )
    }

    if (showEndDateTimePicker) {
        AppDateTimePickerDialog(
            initialDateTime = LocalDateTime.of(form.endDate, LocalTime.NOON),
            title = "选择日期",
            onDismiss = { showEndDateTimePicker = false },
            onConfirm = { dt ->
                showEndDateTimePicker = false
                viewModel.updateForm { s -> s.copy(endDate = dt.toLocalDate()) }
            },
        )
    }
}

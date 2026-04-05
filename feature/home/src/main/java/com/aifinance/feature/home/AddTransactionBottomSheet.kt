package com.aifinance.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aifinance.core.data.repository.ai.AIRepository
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.ExpenseLight
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.IncomeLight
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.state.AIRecognitionResult
import com.aifinance.feature.home.state.AIRecognitionState
import com.aifinance.feature.home.state.ProcessingStep
import com.aifinance.feature.home.util.FileUtils
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        AddTransactionSheetContent(
            onClose = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            },
            onSuccess = {
                scope.launch {
                    sheetState.hide()
                    onSuccess()
                }
            },
            onNavigateToAssetManagement = onNavigateToAssetManagement,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AddTransactionSheetContent(
    onClose: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    viewModel: AddTransactionViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TransactionTab.MANUAL) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf(AppDateTime.now()) }
    var hasEditedDateTime by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<UUID?>(null) }
    var selectedTransferInAccountId by remember { mutableStateOf<UUID?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var accountPickerMode by remember { mutableStateOf(AccountPickerMode.SINGLE) }
    var showTransferSuccessDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            val preferredId = viewModel.resolveDefaultAccountId()
            selectedAccountId = preferredId ?: accounts.first().id
        }
    }

    LaunchedEffect(selectedType) {
        selectedCategory = null
        viewModel.updateType(selectedType)
    }

    LaunchedEffect(selectedType, accounts, selectedAccountId) {
        if (selectedType == TransactionType.TRANSFER && accounts.size >= 2) {
            val sourceId = selectedAccountId
            if (sourceId != null && selectedTransferInAccountId == null) {
                selectedTransferInAccountId = accounts.firstOrNull { it.id != sourceId }?.id
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = OnSurfaceSecondary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "手动记账",
                    selected = selectedTab == TransactionTab.MANUAL,
                    onClick = { selectedTab = TransactionTab.MANUAL }
                )
                TabButton(
                    text = "AI记录",
                    selected = selectedTab == TransactionTab.AI,
                    onClick = { selectedTab = TransactionTab.AI }
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        HorizontalDivider(color = SurfaceSecondary)

        if (selectedTab == TransactionTab.MANUAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeButton(
                    text = "支出",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    selectedColor = ExpenseDefault,
                    modifier = Modifier.weight(1f)
                )
                TypeButton(
                    text = "收入",
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    selectedColor = IncomeDefault,
                    modifier = Modifier.weight(1f)
                )
                TypeButton(
                    text = "转账",
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { selectedType = TransactionType.TRANSFER },
                    selectedColor = BrandPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedType != TransactionType.TRANSFER) {
                Text(
                    text = "选择分类",
                    style = IcokieTextStyles.bodyLarge,
                    color = OnSurfacePrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categories) { category ->
                        CategoryIconWithLabel(
                            icon = category.icon,
                            label = category.name,
                            backgroundColor = Color(category.color),
                            selected = selectedCategory == category.name,
                            onClick = { selectedCategory = category.name }
                        )
                    }
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                val sourceAccount = accounts.firstOrNull { it.id == selectedAccountId }
                val targetAccount = accounts.firstOrNull { it.id == selectedTransferInAccountId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransferAccountCard(
                        modifier = Modifier.weight(1f),
                        title = "扣款账户",
                        account = sourceAccount,
                        onClick = {
                            accountPickerMode = AccountPickerMode.TRANSFER_OUT
                            showAccountPicker = true
                        }
                    )
                    Text(
                        text = "⇄",
                        color = OnSurfaceSecondary,
                        fontSize = 20.sp,
                        modifier = Modifier.clickable {
                            val currentOut = selectedAccountId
                            val currentIn = selectedTransferInAccountId
                            selectedAccountId = currentIn
                            selectedTransferInAccountId = currentOut
                        }
                    )
                    TransferAccountCard(
                        modifier = Modifier.weight(1f),
                        title = "入账账户",
                        account = targetAccount,
                        onClick = {
                            accountPickerMode = AccountPickerMode.TRANSFER_IN
                            showAccountPicker = true
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    InfoChip(
                        icon = "\uD83D\uDCC5",
                        text = formatDate(selectedDateTime.toLocalDate()),
                        onClick = {
                            if (!hasEditedDateTime) {
                                selectedDateTime = AppDateTime.now()
                            }
                            showDateTimePicker = true
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }

                    InfoChip(
                        icon = "\uD83D\uDCB0",
                        text = selectedAccount?.name ?: "选择账户",
                        onClick = {
                            accountPickerMode = AccountPickerMode.SINGLE
                            showAccountPicker = true
                        }
                    )
                    InfoChip(
                        icon = "\uD83D\uDCC5",
                        text = formatDate(selectedDateTime.toLocalDate()),
                        onClick = {
                            if (!hasEditedDateTime) {
                                selectedDateTime = AppDateTime.now()
                            }
                            showDateTimePicker = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "\u00A5",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseDefault
                            TransactionType.INCOME -> IncomeDefault
                            TransactionType.TRANSFER -> BrandPrimary
                        }
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { if (it.length <= 10) amount = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseDefault
                            TransactionType.INCOME -> IncomeDefault
                            TransactionType.TRANSFER -> BrandPrimary
                        },
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                text = "0.00",
                                style = TextStyle(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceTertiary
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (note.isEmpty()) {
                        Text(
                            text = "点击填写备注信息",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfaceTertiary
                        )
                    }
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = IcokieTextStyles.bodyLarge.copy(color = OnSurfacePrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "添加图片",
                            tint = OnSurfaceSecondary
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "拍照",
                            tint = OnSurfaceSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            NumberKeyboard(
                onNumberClick = { digit ->
                    if (amount.length < 10) {
                        amount += digit
                    }
                },
                onDeleteClick = {
                    if (amount.isNotEmpty()) {
                        amount = amount.dropLast(1)
                    }
                },
                onDotClick = {
                    if (!amount.contains(".") && amount.length < 9) {
                        amount += "."
                    }
                },
                onConfirmClick = {
                    if (isSaving || uiState.isLoading) return@NumberKeyboard
                    val accountId = selectedAccountId
                    val transferInAccountId = selectedTransferInAccountId
                    val category = selectedCategory
                    when {
                        amount.isEmpty() || amount == "." -> {
                            Toast.makeText(context, "请输入金额", Toast.LENGTH_SHORT).show()
                        }

                        selectedType != TransactionType.TRANSFER && category == null -> {
                            Toast.makeText(context, "请选择分类", Toast.LENGTH_SHORT).show()
                        }

                        accountId == null -> {
                            accountPickerMode = if (selectedType == TransactionType.TRANSFER) {
                                AccountPickerMode.TRANSFER_OUT
                            } else {
                                AccountPickerMode.SINGLE
                            }
                            showAccountPicker = true
                            Toast.makeText(
                                context,
                                if (selectedType == TransactionType.TRANSFER) "请选择扣款账户" else "请选择账户",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        selectedType == TransactionType.TRANSFER && transferInAccountId == null -> {
                            accountPickerMode = AccountPickerMode.TRANSFER_IN
                            showAccountPicker = true
                            Toast.makeText(context, "请选择入账账户", Toast.LENGTH_SHORT).show()
                        }

                        selectedType == TransactionType.TRANSFER && !isPositiveAmount(amount) -> {
                            Toast.makeText(context, "转账金额必须大于0", Toast.LENGTH_SHORT).show()
                        }

                        selectedType == TransactionType.TRANSFER && accountId == transferInAccountId -> {
                            Toast.makeText(context, "扣款账户和入账账户不能相同", Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            isSaving = true
                            viewModel.saveTransaction(
                                amount = amount,
                                type = selectedType,
                                category = category,
                                note = note,
                                dateTime = selectedDateTime,
                                accountId = accountId,
                                targetAccountId = transferInAccountId,
                            )
                            if (selectedType == TransactionType.TRANSFER) {
                                showTransferSuccessDialog = true
                            } else {
                                onSuccess()
                            }
                        }
                    }
                },
                onSaveAndNewClick = {
                    if (isSaving || uiState.isLoading) return@NumberKeyboard
                    val accountId = selectedAccountId
                    val transferInAccountId = selectedTransferInAccountId
                    val category = selectedCategory
                    when {
                        amount.isEmpty() || amount == "." -> {
                            Toast.makeText(context, "请输入金额", Toast.LENGTH_SHORT).show()
                        }

                        selectedType != TransactionType.TRANSFER && category == null -> {
                            Toast.makeText(context, "请选择分类", Toast.LENGTH_SHORT).show()
                        }

                        accountId == null -> {
                            accountPickerMode = if (selectedType == TransactionType.TRANSFER) {
                                AccountPickerMode.TRANSFER_OUT
                            } else {
                                AccountPickerMode.SINGLE
                            }
                            showAccountPicker = true
                            Toast.makeText(
                                context,
                                if (selectedType == TransactionType.TRANSFER) "请选择扣款账户" else "请选择账户",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        selectedType == TransactionType.TRANSFER && transferInAccountId == null -> {
                            accountPickerMode = AccountPickerMode.TRANSFER_IN
                            showAccountPicker = true
                            Toast.makeText(context, "请选择入账账户", Toast.LENGTH_SHORT).show()
                        }

                        selectedType == TransactionType.TRANSFER && !isPositiveAmount(amount) -> {
                            Toast.makeText(context, "转账金额必须大于0", Toast.LENGTH_SHORT).show()
                        }

                        selectedType == TransactionType.TRANSFER && accountId == transferInAccountId -> {
                            Toast.makeText(context, "扣款账户和入账账户不能相同", Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            isSaving = true
                            viewModel.saveTransaction(
                                amount = amount,
                                type = selectedType,
                                category = category,
                                note = note,
                                dateTime = selectedDateTime,
                                accountId = accountId,
                                targetAccountId = transferInAccountId,
                            )
                            isSaving = false
                            amount = ""
                            selectedCategory = null
                            note = ""
                            selectedDateTime = AppDateTime.now()
                            hasEditedDateTime = false
                        }
                    }
                },
                confirmText = "完成",
                confirmColor = when (selectedType) {
                    TransactionType.EXPENSE -> ExpenseDefault
                    TransactionType.INCOME -> IncomeDefault
                    TransactionType.TRANSFER -> BrandPrimary
                }
            )
        } else {
            val aiRecordViewModel: AIRecordViewModel = hiltViewModel()
            AIRecordContent(
                aiRecordViewModel = aiRecordViewModel,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = {
                    accountPickerMode = AccountPickerMode.SINGLE
                    showAccountPicker = true
                },
                onNavigateToAssetManagement = onNavigateToAssetManagement,
                onSaveTransaction = { amount, type, category, note, dateTime, accountId ->
                    viewModel.saveTransaction(
                        amount = amount,
                        type = type,
                        category = category,
                        note = note,
                        dateTime = dateTime,
                        accountId = accountId
                    )
                    onSuccess()
                }
            )
        }

        if (showAccountPicker) {
            AccountPickerBottomSheet(
                accounts = accounts,
                selectedAccountId = if (accountPickerMode == AccountPickerMode.TRANSFER_IN) {
                    selectedTransferInAccountId
                } else {
                    selectedAccountId
                },
                onDismiss = { showAccountPicker = false },
                onSelect = { account ->
                    when (accountPickerMode) {
                        AccountPickerMode.SINGLE, AccountPickerMode.TRANSFER_OUT -> {
                            selectedAccountId = account.id
                        }

                        AccountPickerMode.TRANSFER_IN -> {
                            selectedTransferInAccountId = account.id
                        }
                    }
                    showAccountPicker = false
                },
                onNavigateToAssetManagement = {
                    showAccountPicker = false
                    onNavigateToAssetManagement()
                }
            )
        }

        if (showTransferSuccessDialog) {
            TransferSuccessDialog(
                dateTime = selectedDateTime,
                onDismiss = {
                    isSaving = false
                    showTransferSuccessDialog = false
                    onSuccess()
                },
                onGoSee = {
                    isSaving = false
                    showTransferSuccessDialog = false
                    onSuccess()
                }
            )
        }

        if (showDateTimePicker) {
            AppDateTimePickerDialog(
                initialDateTime = selectedDateTime,
                title = "选择日期",
                onDismiss = { showDateTimePicker = false },
                onConfirm = { dateTime ->
                    val changedByUser = dateTime != selectedDateTime
                    selectedDateTime = dateTime
                    if (changedByUser) {
                        hasEditedDateTime = true
                    }
                    showDateTimePicker = false
                }
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SurfacePrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (selected) IcokieTextStyles.labelMedium else IcokieTextStyles.labelSmall,
            color = if (selected) OnSurfacePrimary else OnSurfaceSecondary
        )
    }
}

@Composable
private fun TypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) selectedColor.copy(alpha = 0.1f) else SurfaceSecondary
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium,
            color = if (selected) selectedColor else OnSurfaceSecondary
        )
    }
}

@Composable
private fun TextButtonWithIcon(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium,
            color = BrandPrimary
        )
        Text(
            text = "\u2191",
            style = IcokieTextStyles.labelMedium,
            color = BrandPrimary
        )
    }
}

@Composable
private fun CategoryIconWithLabel(
    icon: String,
    label: String,
    backgroundColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) BrandPrimary else backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
        }

        Text(
            text = label,
            style = IcokieTextStyles.labelSmall,
            color = if (selected) BrandPrimary else OnSurfaceSecondary
        )
    }
}

@Composable
private fun InfoChip(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = text,
            style = IcokieTextStyles.labelSmall,
            color = OnSurfaceSecondary
        )
    }
}

@Composable
private fun TransferAccountCard(
    title: String,
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = account?.let { Color(it.color).copy(alpha = 0.14f) } ?: SurfaceSecondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (account == null) {
            Text(
                text = title,
                style = IcokieTextStyles.bodyLarge,
                color = OnSurfaceSecondary,
                maxLines = 1
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = account.icon, fontSize = 22.sp)
                Column {
                    Text(
                        text = account.name,
                        style = IcokieTextStyles.bodyLarge,
                        color = OnSurfacePrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "余额 ${account.currentBalance}",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerBottomSheet(
    accounts: List<Account>,
    selectedAccountId: UUID?,
    onDismiss: () -> Unit,
    onSelect: (Account) -> Unit,
    onNavigateToAssetManagement: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "选择账户", style = IcokieTextStyles.titleLarge, color = OnSurfacePrimary)
                Text(
                    text = "+ 添加账户",
                    style = IcokieTextStyles.labelMedium,
                    color = BrandPrimary,
                    modifier = Modifier.clickable(onClick = onNavigateToAssetManagement)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceSecondary)
                        .clickable(onClick = onNavigateToAssetManagement)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "暂无账户，点击添加账户",
                        style = IcokieTextStyles.bodyLarge,
                        color = OnSurfaceSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        val selected = account.id == selectedAccountId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) BrandPrimary.copy(alpha = 0.08f) else SurfaceSecondary)
                                .clickable { onSelect(account) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(text = account.icon, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = account.name,
                                        style = IcokieTextStyles.bodyLarge,
                                        color = OnSurfacePrimary,
                                    )
                                    Text(
                                        text = account.note.takeUnless { it.isNullOrBlank() } ?: "可用于默认收支",
                                        style = IcokieTextStyles.labelSmall,
                                        color = OnSurfaceSecondary,
                                    )
                                }
                            }
                            Text(
                                text = "¥${account.currentBalance}",
                                style = IcokieTextStyles.labelMedium,
                                color = OnSurfacePrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberKeyboard(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDotClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onSaveAndNewClick: () -> Unit,
    confirmText: String,
    confirmColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSecondary)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..3) {
                NumberKey(
                    text = i.toString(),
                    onClick = { onNumberClick(i.toString()) },
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfacePrimary)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u232B",
                    style = TextStyle(fontSize = 20.sp),
                    color = OnSurfacePrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 4..6) {
                NumberKey(
                    text = i.toString(),
                    onClick = { onNumberClick(i.toString()) },
                    modifier = Modifier.weight(1f)
                )
            }
            NumberKey(
                text = "+",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 7..9) {
                NumberKey(
                    text = i.toString(),
                    onClick = { onNumberClick(i.toString()) },
                    modifier = Modifier.weight(1f)
                )
            }
            NumberKey(
                text = "-",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfacePrimary)
                    .clickable(onClick = onSaveAndNewClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "再记",
                    style = IcokieTextStyles.labelMedium,
                    color = BrandPrimary
                )
            }

            NumberKey(
                text = "0",
                onClick = { onNumberClick("0") },
                modifier = Modifier.weight(1f)
            )

            NumberKey(
                text = ".",
                onClick = onDotClick,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onConfirmClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = confirmText,
                    style = IcokieTextStyles.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun NumberKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfacePrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfacePrimary
            )
        )
    }
}

@Composable
private fun TransferSuccessDialog(
    dateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onGoSee: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "记录成功！",
                    style = IcokieTextStyles.titleLarge,
                    color = OnSurfacePrimary
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "时间：${formatDateTime(dateTime)}",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary
                        )
                        Text(
                            text = "分类：转账",
                            style = IcokieTextStyles.bodyLarge,
                            color = OnSurfacePrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceSecondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("知道了", style = IcokieTextStyles.labelMedium, color = OnSurfaceSecondary)
                    }
                    Button(
                        onClick = onGoSee,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("去看看", style = IcokieTextStyles.labelMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AIRecordContent(
    aiRecordViewModel: AIRecordViewModel,
    accounts: List<Account>,
    selectedAccountId: UUID?,
    onSelectAccount: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    onSaveTransaction: (
        amount: String,
        type: TransactionType,
        category: String?,
        note: String,
        dateTime: LocalDateTime,
        accountId: UUID
    ) -> Unit
) {
    val context = LocalContext.current
    val aiState by aiRecordViewModel.uiState.collectAsStateWithLifecycle()
    
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val file = FileUtils.uriToFile(context, it)
            if (file != null) {
                aiRecordViewModel.processImage(it, file)
            } else {
                Toast.makeText(context, "无法读取图片文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val file = FileUtils.uriToFile(context, uri)
                if (file != null) {
                    aiRecordViewModel.processImage(uri, file)
                } else {
                    Toast.makeText(context, "无法读取图片文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val state = aiState) {
            is AIRecognitionState.Idle -> {
                AIRecordIdleView(
                    onCameraClick = {
                        val uri = FileUtils.createTempImageUri(context)
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onGalleryClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
            is AIRecognitionState.Processing -> {
                AIRecordProcessingView(step = state.step)
            }
            is AIRecognitionState.Success -> {
                AIRecordSuccessView(
                    result = state.result,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onSelectAccount = onSelectAccount,
                    onNavigateToAssetManagement = onNavigateToAssetManagement,
                    onSave = { amount, type, category, note, dateTime, accountId ->
                        onSaveTransaction(amount, type, category, note, dateTime, accountId)
                        aiRecordViewModel.reset()
                    },
                    onCancel = {
                        aiRecordViewModel.reset()
                    }
                )
            }
            is AIRecognitionState.Error -> {
                AIRecordErrorView(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = { aiRecordViewModel.retry() },
                    onDismiss = { aiRecordViewModel.reset() }
                )
            }
        }
    }
}

@Composable
private fun AIRecordIdleView(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🤖 AI智能识别",
            style = IcokieTextStyles.titleLarge,
            color = OnSurfacePrimary
        )
        Text(
            text = "拍照或选择图片，AI自动识别账单信息",
            style = IcokieTextStyles.bodyLarge,
            color = OnSurfaceSecondary
        )

        Button(
            onClick = onCameraClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("拍照识别")
        }

        Button(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = OnSurfacePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("从相册选择", color = OnSurfacePrimary)
        }
    }
}

@Composable
private fun AIRecordProcessingView(step: ProcessingStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = BrandPrimary
        )
        
        val stepText = when (step) {
            ProcessingStep.UPLOADING -> "正在上传图片..."
            ProcessingStep.RECOGNIZING -> "正在识别文字..."
            ProcessingStep.PARSING -> "正在解析账单..."
        }
        
        Text(
            text = stepText,
            style = IcokieTextStyles.bodyLarge,
            color = OnSurfacePrimary
        )
        
        Text(
            text = "请稍候，AI正在处理您的账单",
            style = IcokieTextStyles.bodyMedium,
            color = OnSurfaceSecondary
        )
    }
}

@Composable
private fun AIRecordSuccessView(
    result: AIRecognitionResult,
    accounts: List<Account>,
    selectedAccountId: UUID?,
    onSelectAccount: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    onSave: (
        amount: String,
        type: TransactionType,
        category: String?,
        note: String,
        dateTime: LocalDateTime,
        accountId: UUID
    ) -> Unit,
    onCancel: () -> Unit
) {
    val lowConfidenceColor = ExpenseDefault
    val normalColor = OnSurfaceSecondary

    val isLowConfidence = { _: String, confidence: Float ->
        confidence < 0.7f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "✅ 识别成功",
            style = IcokieTextStyles.titleLarge,
            color = IncomeDefault
        )

        if (result.confidence.isLowConfidence()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ExpenseDefault.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "部分字段置信度较低，请核对后保存",
                        style = IcokieTextStyles.bodyMedium,
                        color = ExpenseDefault
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val amountConfidence = result.confidence?.amount ?: 1f
                Text(
                    text = "金额: ¥${result.amount}",
                    style = IcokieTextStyles.bodyLarge,
                    color = if (isLowConfidence("amount", amountConfidence)) lowConfidenceColor else OnSurfacePrimary
                )

                val typeConfidence = result.confidence?.type ?: 1f
                Text(
                    text = "类型: ${if (result.type == TransactionType.INCOME) "收入" else "支出"}",
                    style = IcokieTextStyles.bodyMedium,
                    color = if (isLowConfidence("type", typeConfidence)) lowConfidenceColor else normalColor
                )

                if (result.category.isNotEmpty()) {
                    val categoryConfidence = result.confidence?.category ?: 1f
                    Text(
                        text = "分类: ${result.category}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("category", categoryConfidence)) lowConfidenceColor else normalColor
                    )
                }

                if (result.merchant.isNotEmpty()) {
                    val merchantConfidence = result.confidence?.merchant ?: 1f
                    Text(
                        text = "商家: ${result.merchant}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("merchant", merchantConfidence)) lowConfidenceColor else normalColor
                    )
                }

                val dateConfidence = result.confidence?.date ?: 1f
                Text(
                    text = "日期: ${result.date}",
                    style = IcokieTextStyles.bodyMedium,
                    color = if (isLowConfidence("date", dateConfidence)) lowConfidenceColor else normalColor
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SurfacePrimary
                )

                if (!result.paymentTime.isNullOrEmpty()) {
                    val paymentTimeConfidence = result.confidence?.paymentTime ?: 1f
                    Text(
                        text = "支付时间: ${result.paymentTime}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("paymentTime", paymentTimeConfidence)) lowConfidenceColor else normalColor
                    )
                }

                if (!result.paymentMethod.isNullOrEmpty()) {
                    val paymentMethodConfidence = result.confidence?.paymentMethod ?: 1f
                    Text(
                        text = "支付方式: ${result.paymentMethod}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("paymentMethod", paymentMethodConfidence)) lowConfidenceColor else normalColor
                    )
                }

                if (!result.paymentAccount.isNullOrEmpty()) {
                    val paymentAccountConfidence = result.confidence?.paymentAccount ?: 1f
                    Text(
                        text = "支付账户: ${result.paymentAccount}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("paymentAccount", paymentAccountConfidence)) lowConfidenceColor else normalColor
                    )
                }

                if (!result.description.isNullOrEmpty()) {
                    val descriptionConfidence = result.confidence?.description ?: 1f
                    Text(
                        text = "描述: ${result.description}",
                        style = IcokieTextStyles.bodyMedium,
                        color = if (isLowConfidence("description", descriptionConfidence)) lowConfidenceColor else normalColor
                    )
                }
            }
        }
        
        val selectedAccount = accounts.find { it.id == selectedAccountId }
        
        if (selectedAccount != null) {
            Button(
                onClick = {
                    val dateTime = try {
                        val date = LocalDate.parse(result.date)
                        val time = result.paymentTime?.takeIf { it.isNotEmpty() }?.let {
                            try {
                                LocalTime.parse(it)
                            } catch (_: Exception) {
                                LocalTime.MIN
                            }
                        } ?: LocalTime.MIN
                        LocalDateTime.of(date, time)
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }
                    onSave(
                        result.amount.toString(),
                        result.type,
                        result.category.ifEmpty { null },
                        result.merchant,
                        dateTime,
                        selectedAccount.id
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存到 ${selectedAccount.name}")
            }
        } else {
            Button(
                onClick = onSelectAccount,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("选择账户")
            }
        }
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("取消", color = OnSurfacePrimary)
        }
    }
}

@Composable
private fun AIRecordErrorView(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "❌ 识别失败",
            style = IcokieTextStyles.titleLarge,
            color = ExpenseDefault
        )
        
        Text(
            text = message,
            style = IcokieTextStyles.bodyMedium,
            color = OnSurfaceSecondary,
            textAlign = TextAlign.Center
        )
        
        if (canRetry) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重试")
            }
        }
        
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("取消", color = OnSurfacePrimary)
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return "${date.monthValue}\u6708${date.dayOfMonth}\u65E5"
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss")
    return dateTime.format(formatter)
}

private fun isPositiveAmount(amount: String): Boolean {
    val value = amount.toBigDecimalOrNull() ?: return false
    return value > BigDecimal.ZERO
}

data class CategoryItem(
    val name: String,
    val icon: String,
    val color: Color
)

enum class TransactionTab {
    MANUAL, AI
}

private enum class AccountPickerMode {
    SINGLE,
    TRANSFER_OUT,
    TRANSFER_IN,
}

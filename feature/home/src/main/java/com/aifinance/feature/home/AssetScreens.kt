package com.aifinance.feature.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.Account
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

const val ASSET_MANAGEMENT_ROUTE = "asset_management"
const val ADD_ASSET_ACCOUNT_ROUTE = "add_asset_account"
const val ADD_ASSET_DETAIL_ROUTE = "add_asset_detail/{presetKey}"
const val EDIT_ASSET_ACCOUNT_ROUTE = "edit_asset_account/{accountId}"

fun addAssetDetailRoute(presetKey: String): String = "add_asset_detail/$presetKey"
fun editAssetAccountRoute(accountId: UUID): String = "edit_asset_account/$accountId"

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AssetManagementScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (UUID) -> Unit,
    viewModel: AssetManagementViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产管理", style = IcokieTextStyles.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = "+ 添加账户",
                        color = BrandPrimary,
                        style = IcokieTextStyles.labelMedium,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable(onClick = onAddAccount)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePrimary)
            )
        },
        containerColor = SurfaceSecondary,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E6A3)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("净资产", style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)
                            Text("资产管理", style = IcokieTextStyles.labelMedium, color = OnSurfacePrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¥${summary.netAssets.setScale(2, RoundingMode.HALF_UP).toPlainString()}",
                            style = IcokieTextStyles.headlineLarge,
                            color = OnSurfacePrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("资产", style = IcokieTextStyles.labelSmall, color = OnSurfaceSecondary)
                                Text(
                                    "¥${summary.assets.setScale(2, RoundingMode.HALF_UP).toPlainString()}",
                                    style = IcokieTextStyles.titleMedium,
                                    color = OnSurfacePrimary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("负债", style = IcokieTextStyles.labelSmall, color = OnSurfaceSecondary)
                                Text(
                                    "¥${summary.liabilities.setScale(2, RoundingMode.HALF_UP).toPlainString()}",
                                    style = IcokieTextStyles.titleMedium,
                                    color = OnSurfacePrimary,
                                )
                            }
                        }
                    }
                }
            }

            items(accounts, key = { it.id }) { account ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onAccountClick(account.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceSecondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(account.icon)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (account.isDefaultIncomeExpense) {
                                        Text(
                                            text = "默认",
                                            style = IcokieTextStyles.labelSmall,
                                            color = BrandPrimary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BrandPrimary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Text(account.name, style = IcokieTextStyles.titleMedium, color = OnSurfacePrimary)
                                }
                                Text(
                                    text = account.note.takeUnless { it.isNullOrBlank() } ?: account.type.toDisplayName(),
                                    style = IcokieTextStyles.labelSmall,
                                    color = OnSurfaceSecondary,
                                )
                            }
                        }
                        Text(
                            text = "¥${account.currentBalance.setScale(2, RoundingMode.HALF_UP).toPlainString()}",
                            style = IcokieTextStyles.titleMedium,
                            color = OnSurfacePrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddAssetAccountScreen(
    onBack: () -> Unit,
    onPresetClick: (String) -> Unit,
) {
    val sections = remember { accountPresetSections() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加账户", style = IcokieTextStyles.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePrimary)
            )
        },
        containerColor = SurfaceSecondary,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            sections.forEach { section ->
                item {
                    Text(section.title, style = IcokieTextStyles.titleMedium, color = OnSurfacePrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(section.presets) { preset ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onPresetClick(preset.key) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(SurfacePrimary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = preset.icon, style = IcokieTextStyles.titleLarge)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = preset.name, style = IcokieTextStyles.labelSmall, color = OnSurfaceSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddAssetDetailScreen(
    presetKey: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AssetManagementViewModel = hiltViewModel(),
) {
    val preset = remember(presetKey) { accountPresetByKey(presetKey) ?: AccountPreset.customAsset() }

    var amount by remember { mutableStateOf("0.00") }
    var accountName by remember { mutableStateOf(preset.defaultAccountName) }
    var note by remember { mutableStateOf("") }
    var includeInTotalAssets by remember { mutableStateOf(true) }
    var isDefaultIncomeExpense by remember { mutableStateOf(false) }
    var startDateTime by remember { mutableStateOf(AppDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = accountName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加账户", style = IcokieTextStyles.titleLarge) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E6A3))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("起始余额", style = IcokieTextStyles.titleMedium)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value -> amount = value.filter { ch -> ch.isDigit() || ch == '.' } },
                        modifier = Modifier.width(140.dp),
                        leadingIcon = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "起始时间（必填）",
                        subtitle = "该时间之前的记录将不计入余额统计",
                        trailing = formatDateTime(startDateTime),
                        onClick = { showDatePicker = true }
                    )
                    HorizontalDivider(color = SurfaceSecondary)
                    SettingRow(
                        title = "账户名称",
                        subtitle = null,
                        trailing = accountName,
                        onClick = {}
                    )
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        placeholder = { Text("请输入账户名称") },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SurfaceSecondary)
                    SettingRow(
                        title = "备注",
                        subtitle = null,
                        trailing = note.ifBlank { "请输入备注" },
                        onClick = {}
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        placeholder = { Text("请输入备注") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                Column {
                    SwitchRow(
                        title = "计入总资产",
                        subtitle = "该账户是否计入总资产",
                        checked = includeInTotalAssets,
                        onCheckedChange = { includeInTotalAssets = it },
                    )
                    HorizontalDivider(color = SurfaceSecondary)
                    SwitchRow(
                        title = "设为默认收支账户",
                        subtitle = "若收支记录没有指定账户，会默认关联到该账户",
                        checked = isDefaultIncomeExpense,
                        onCheckedChange = { isDefaultIncomeExpense = it },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.viewModelScope.launch {
                        viewModel.addAccount(
                            preset = preset,
                            accountName = accountName,
                            note = note,
                            amount = amount,
                            includeInTotalAssets = includeInTotalAssets,
                            isDefaultIncomeExpense = isDefaultIncomeExpense,
                            startDateTime = startDateTime,
                        )
                        onSaved()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
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

    if (showDatePicker) {
        AppDateTimePickerDialog(
            initialDateTime = startDateTime,
            title = "选择日期",
            onDismiss = { showDatePicker = false },
            onConfirm = {
                startDateTime = it
                showDatePicker = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditAssetAccountScreen(
    accountId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AssetManagementViewModel = hiltViewModel(),
) {
    var account by remember { mutableStateOf<Account?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        val uuid = try { UUID.fromString(accountId) } catch (e: Exception) { null }
        account = uuid?.let { viewModel.getAccountById(it) }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...", style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)
        }
        return
    }

    if (account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("账户不存在", style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)
        }
        return
    }

    val currentAccount = account!!
    var amount by remember { mutableStateOf(currentAccount.currentBalance.abs().setScale(2, RoundingMode.HALF_UP).toPlainString()) }
    var accountName by remember { mutableStateOf(currentAccount.name) }
    var note by remember { mutableStateOf(currentAccount.note ?: "") }
    var includeInTotalAssets by remember { mutableStateOf(currentAccount.includeInTotalAssets) }
    var isDefaultIncomeExpense by remember { mutableStateOf(currentAccount.isDefaultIncomeExpense) }

    val canSave = accountName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑账户", style = IcokieTextStyles.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = "删除",
                        color = Color(0xFFDC2626),
                        style = IcokieTextStyles.labelMedium,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { showDeleteDialog = true }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePrimary),
            )
        },
        containerColor = SurfaceSecondary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E6A3))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("当前余额", style = IcokieTextStyles.titleMedium)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value -> amount = value.filter { ch -> ch.isDigit() || ch == '.' } },
                        modifier = Modifier.width(140.dp),
                        leadingIcon = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "账户名称",
                        subtitle = null,
                        trailing = accountName,
                        onClick = {}
                    )
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        placeholder = { Text("请输入账户名称") },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SurfaceSecondary)
                    SettingRow(
                        title = "备注",
                        subtitle = null,
                        trailing = note.ifBlank { "请输入备注" },
                        onClick = {}
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        placeholder = { Text("请输入备注") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfacePrimary)) {
                Column {
                    SwitchRow(
                        title = "计入总资产",
                        subtitle = "该账户是否计入总资产",
                        checked = includeInTotalAssets,
                        onCheckedChange = { includeInTotalAssets = it },
                    )
                    HorizontalDivider(color = SurfaceSecondary)
                    SwitchRow(
                        title = "设为默认收支账户",
                        subtitle = "若收支记录没有指定账户，会默认关联到该账户",
                        checked = isDefaultIncomeExpense,
                        onCheckedChange = { isDefaultIncomeExpense = it },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.viewModelScope.launch {
                        viewModel.updateAccount(
                            account = currentAccount,
                            accountName = accountName,
                            note = note,
                            amount = amount,
                            includeInTotalAssets = includeInTotalAssets,
                            isDefaultIncomeExpense = isDefaultIncomeExpense,
                        )
                        onSaved()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    disabledContainerColor = OnSurfaceTertiary.copy(alpha = 0.3f),
                ),
            ) {
                Text("保存修改", style = IcokieTextStyles.titleMedium, color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", style = IcokieTextStyles.titleMedium) },
            text = { Text("确定要删除账户\"$accountName\"吗？此操作不可恢复。", style = IcokieTextStyles.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.deleteAccount(currentAccount)
                            showDeleteDialog = false
                            onSaved()
                        }
                    }
                ) {
                    Text("删除", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = SurfacePrimary
        )
    }
}

@HiltViewModel
class AssetManagementViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : androidx.lifecycle.ViewModel() {
    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val summary: StateFlow<AssetSummary> = accounts
        .map { accountList ->
            val assets = accountList
                .filter { it.includeInTotalAssets && !it.type.isLiability() }
                .fold(BigDecimal.ZERO) { acc, account -> acc + account.currentBalance }
            val liabilities = accountList
                .filter { it.includeInTotalAssets && it.type.isLiability() }
                .fold(BigDecimal.ZERO) { acc, account -> acc + account.currentBalance.abs() }
            AssetSummary(
                assets = assets,
                liabilities = liabilities,
                netAssets = assets - liabilities,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssetSummary(),
        )

    suspend fun addAccount(
        preset: AccountPreset,
        accountName: String,
        note: String,
        amount: String,
        includeInTotalAssets: Boolean,
        isDefaultIncomeExpense: Boolean,
        startDateTime: LocalDateTime,
    ) {
        val parsedAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val normalizedAmount = if (preset.type.isLiability()) parsedAmount.abs().negate() else parsedAmount

        accountRepository.insertAccount(
            Account(
                id = UUID.randomUUID(),
                name = accountName,
                type = preset.type,
                currency = "CNY",
                initialBalance = normalizedAmount,
                currentBalance = normalizedAmount,
                color = preset.color,
                icon = preset.icon,
                note = note,
                isArchived = false,
                includeInTotalAssets = includeInTotalAssets,
                isDefaultIncomeExpense = isDefaultIncomeExpense,
                createdAt = AppDateTime.toInstant(startDateTime),
                updatedAt = AppDateTime.toInstant(startDateTime),
            )
        )
    }

    suspend fun getAccountById(id: UUID): Account? {
        return accountRepository.getAccountById(id)
    }

    suspend fun updateAccount(
        account: Account,
        accountName: String,
        note: String,
        amount: String,
        includeInTotalAssets: Boolean,
        isDefaultIncomeExpense: Boolean,
    ) {
        val parsedAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val normalizedAmount = if (account.type.isLiability()) parsedAmount.abs().negate() else parsedAmount

        accountRepository.updateAccount(
            account.copy(
                name = accountName,
                note = note,
                initialBalance = normalizedAmount,
                currentBalance = normalizedAmount,
                includeInTotalAssets = includeInTotalAssets,
                isDefaultIncomeExpense = isDefaultIncomeExpense,
                updatedAt = Instant.now(),
            )
        )
    }

    suspend fun deleteAccount(account: Account) {
        accountRepository.deleteAccount(account)
    }
}

data class AssetSummary(
    val assets: BigDecimal = BigDecimal.ZERO,
    val liabilities: BigDecimal = BigDecimal.ZERO,
    val netAssets: BigDecimal = BigDecimal.ZERO,
)

private data class AccountPresetSection(
    val title: String,
    val presets: List<AccountPreset>,
)

data class AccountPreset(
    val key: String,
    val name: String,
    val icon: String,
    val type: AccountType,
    val defaultAccountName: String,
    val color: Int,
) {
    companion object {
        fun customAsset(): AccountPreset {
            return AccountPreset(
                key = "custom_asset",
                name = "自定义",
                icon = "💬",
                type = AccountType.OTHER,
                defaultAccountName = "自定义账户",
                color = 0xFF9CA3AF.toInt(),
            )
        }
    }
}

private fun accountPresetSections(): List<AccountPresetSection> {
    return listOf(
        AccountPresetSection(
            title = "💰资金账户（资产）",
            presets = listOf(
                AccountPreset("saving_card", "储蓄卡", "💳", AccountType.BANK, "储蓄卡", 0xFF2563EB.toInt()),
                AccountPreset("wechat", "微信", "💬", AccountType.DIGITAL_WALLET, "微信", 0xFF22C55E.toInt()),
                AccountPreset("alipay", "支付宝", "🔵", AccountType.DIGITAL_WALLET, "支付宝", 0xFF0EA5E9.toInt()),
                AccountPreset("cash", "现金", "💰", AccountType.CASH, "现金", 0xFFF59E0B.toInt()),
                AccountPreset.customAsset(),
            )
        ),
        AccountPresetSection(
            title = "💳信用账户（负债）",
            presets = listOf(
                AccountPreset("credit_card", "信用卡", "💳", AccountType.CREDIT_CARD, "信用卡", 0xFFF59E0B.toInt()),
                AccountPreset("huabei", "花呗", "🔵", AccountType.CREDIT_CARD, "花呗", 0xFF3B82F6.toInt()),
                AccountPreset("jd_baitiao", "京东白条", "🟧", AccountType.CREDIT_CARD, "京东白条", 0xFFFB923C.toInt()),
                AccountPreset("jiebei", "借呗", "🐧", AccountType.CREDIT_CARD, "借呗", 0xFF2563EB.toInt()),
            )
        ),
        AccountPresetSection(
            title = "📈理财账户（资产）",
            presets = listOf(
                AccountPreset("stock", "股票", "📈", AccountType.INVESTMENT, "股票账户", 0xFFEF4444.toInt()),
                AccountPreset("fund", "基金", "🟠", AccountType.INVESTMENT, "基金账户", 0xFFEA580C.toInt()),
                AccountPreset("yu_e_bao", "余额宝", "🟡", AccountType.INVESTMENT, "余额宝", 0xFFEAB308.toInt()),
                AccountPreset("ling_qian_tong", "零钱通", "💎", AccountType.INVESTMENT, "零钱通", 0xFF22C55E.toInt()),
            )
        ),
    )
}

fun accountPresetByKey(key: String): AccountPreset? {
    return accountPresetSections().flatMap { it.presets }.firstOrNull { it.key == key }
}

private fun AccountType.isLiability(): Boolean {
    return this == AccountType.CREDIT_CARD
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    trailing: String,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = IcokieTextStyles.bodyLarge, color = OnSurfacePrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = IcokieTextStyles.labelSmall, color = OnSurfaceTertiary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trailing, style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)
            Spacer(modifier = Modifier.width(4.dp))
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
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = IcokieTextStyles.bodyLarge, color = OnSurfacePrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = IcokieTextStyles.labelSmall, color = OnSurfaceTertiary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun AccountType.toDisplayName(): String {
    return when (this) {
        AccountType.CASH -> "现金"
        AccountType.BANK -> "储蓄卡"
        AccountType.CREDIT_CARD -> "信用账户"
        AccountType.INVESTMENT -> "理财账户"
        AccountType.DIGITAL_WALLET -> "数字钱包"
        AccountType.OTHER -> "其他"
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    return "%d-%02d-%02d %02d:%02d".format(
        dateTime.year,
        dateTime.monthValue,
        dateTime.dayOfMonth,
        dateTime.hour,
        dateTime.minute,
    )
}

package com.aifinance.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.BrandPrimaryLight
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IcokieTheme
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.component.CategoryPickerBottomSheet
import com.aifinance.feature.home.component.GradientGlassCard
import com.aifinance.feature.home.component.toOption
import com.aifinance.feature.home.component.NetAssetGradientCard
import com.aifinance.feature.home.component.MonthlyExpenseGradientCard
import com.aifinance.feature.home.component.RefinedTransactionItem
import com.aifinance.feature.home.component.SwipeableTransactionItem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordHomeContent(
    onNavigateToAssetManagement: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val spacing = IcokieTheme.spacing

    var showAddTransaction by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now()) }
    var categoryPickerTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transientSuccessMessage by remember { mutableStateOf<String?>(null) }

    val recentTransactions = viewModel.recentTransactions.collectAsStateWithLifecycle()
    val totalBalance = viewModel.totalBalance.collectAsStateWithLifecycle()
    val accountsById = viewModel.accountsById.collectAsStateWithLifecycle()
    val categoriesById = viewModel.categoriesById.collectAsStateWithLifecycle()
    val filteredTransactions = remember(recentTransactions.value, selectedMonth) {
        recentTransactions.value.filter {
            it.date.year == selectedMonth.year && it.date.monthValue == selectedMonth.monthValue
        }
    }
    val displayIncome = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.INCOME && !it.isPending }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    }
    val displayExpense = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.EXPENSE && !it.isPending }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    }

    LaunchedEffect(transientSuccessMessage) {
        if (transientSuccessMessage != null) {
            delay(1600)
            transientSuccessMessage = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransaction = true },
                containerColor = BrandPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "记一笔",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = spacing.pagePadding,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
            ) {
                item {
                    BalanceCard(
                        balance = totalBalance.value.balance,
                        selectedMonth = selectedMonth,
                        assets = totalBalance.value.assets,
                        liabilities = totalBalance.value.liabilities,
                        income = displayIncome,
                        expense = displayExpense,
                        onAssetManageClick = onNavigateToAssetManagement,
                        onStatisticsClick = onNavigateToStatistics,
                    )
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyRecentTransactions()
                    }
                } else {
                    val groupedByDate = filteredTransactions.groupBy { it.date }
                    groupedByDate.toSortedMap(compareByDescending<LocalDate> { it }).forEach { (date, dayTransactions) ->
                        val sortedTransactions = dayTransactions.sortedByDescending { it.time }
                        item(key = "home-day-$date") {
                            DaySectionHeader(
                                date = date,
                                dayTransactions = dayTransactions,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF7F8FC),
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                sortedTransactions.forEachIndexed { index, transaction ->
                                    SwipeableTransactionItem(
                                        transaction = transaction,
                                        accountName = accountsById.value[transaction.accountId]?.name,
                                        category = categoriesById.value[transaction.categoryId],
                                        onClick = { onNavigateToTransactionDetail(transaction.id) },
                                        onAmountClick = { onNavigateToTransactionDetail(transaction.id) },
                                        onCategoryClick = { categoryPickerTransaction = transaction },
                                        onDeleteClick = {
                                            pendingDeleteTransaction = transaction
                                        },
                                    )
                                    if (index < sortedTransactions.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = Color(0x143C3C43),
                                            modifier = Modifier.padding(start = 16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            transientSuccessMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCC1F2937),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = IcokieTextStyles.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }

        if (showAddTransaction) {
            AddTransactionBottomSheet(
                onDismiss = { showAddTransaction = false },
                onSuccess = {
                    showAddTransaction = false
                },
                onNavigateToAssetManagement = onNavigateToAssetManagement,
            )
        }

        categoryPickerTransaction?.let { transaction ->
            val categoriesForPicker: List<com.aifinance.feature.home.component.CategoryOption> = remember(categoriesById.value, transaction.type) {
                val defaults = CategoryCatalog.forType(transaction.type)
                    .map { it.toOption() }
                val customForType = categoriesById.value.values
                    .filter { it.type == transaction.type && !it.isDefault }
                    .sortedBy { it.order }
                    .map { it.toOption() }
                defaults + customForType
            }
            CategoryPickerBottomSheet(
                selectedCategoryId = transaction.categoryId,
                categories = categoriesForPicker,
                onDismiss = { categoryPickerTransaction = null },
                onSelect = { categoryId ->
                    val selectedCategory = categoriesById.value[categoryId]
                    viewModel.updateTransactionCategory(
                        transaction = transaction,
                        categoryId = categoryId,
                        categoryName = selectedCategory?.name,
                    )
                    categoryPickerTransaction = null
                },
            )
        }

        pendingDeleteTransaction?.let { transaction ->
            AlertDialog(
                onDismissRequest = { pendingDeleteTransaction = null },
                title = {
                    Text(
                        text = "删除记录",
                        style = IcokieTextStyles.titleMedium,
                    )
                },
                text = {
                    Text(
                        text = "确认删除这条账单记录吗？",
                        style = IcokieTextStyles.bodyLarge,
                        color = OnSurfaceSecondary,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(transaction)
                            pendingDeleteTransaction = null
                            transientSuccessMessage = "删除成功"
                        }
                    ) {
                        Text(text = "删除", color = ExpenseDefault)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTransaction = null }) {
                        Text(text = "取消")
                    }
                },
                containerColor = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@Composable
private fun AIAlertCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    elevation: androidx.compose.ui.unit.Dp,
    alertLevel: AlertLevel = AlertLevel.NORMAL,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    val backgroundColor = when (alertLevel) {
        AlertLevel.NORMAL -> BrandPrimary
        AlertLevel.WARNING -> Color(0xFFF59E0B)
        AlertLevel.CRITICAL -> ExpenseDefault
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OnPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "AI Alert",
                        tint = OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = IcokieTextStyles.titleMedium,
                        color = OnPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.elementSpacing))
                    Text(
                        text = subtitle,
                        style = IcokieTextStyles.labelMedium,
                        color = OnPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "查看详情",
                tint = OnPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BalanceCard(
    balance: BigDecimal,
    selectedMonth: LocalDate,
    assets: BigDecimal,
    liabilities: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    onAssetManageClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation = IcokieTheme.elevation
    var hideAmount by remember { mutableStateOf(false) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    var indicatorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
        indicatorVisible = false
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            indicatorVisible = true
        } else {
            delay(900)
            indicatorVisible = false
        }
    }

    fun secureMoney(value: BigDecimal): String {
        val amountText = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        return if (hideAmount) "******" else "¥$amountText"
    }

    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp)
            ) {
                if (page == 0) {
                    NetAssetGlassCard(
                        balance = balance,
                        assets = assets,
                        liabilities = liabilities,
                        hideAmount = hideAmount,
                        onToggleHide = { hideAmount = !hideAmount },
                        onAssetManageClick = onAssetManageClick,
                        secureMoney = ::secureMoney,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        elevation = elevation.cardElevation,
                    )
                } else {
                    MonthlyExpenseGlassCard(
                        selectedMonth = selectedMonth,
                        income = income,
                        expense = expense,
                        hideAmount = hideAmount,
                        onToggleHide = { hideAmount = !hideAmount },
                        onStatisticsClick = onStatisticsClick,
                        secureMoney = ::secureMoney,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        elevation = elevation.cardElevation,
                    )
                }
            }
        }

        val indicatorAlpha = if (indicatorVisible || pagerState.isScrollInProgress) 0.95f else 0.12f
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = if (pagerState.currentPage == index) 12.dp else 7.dp, height = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (pagerState.currentPage == index) {
                                OnSurfacePrimary.copy(alpha = indicatorAlpha)
                            } else {
                                OnSurfaceTertiary.copy(alpha = 0.35f * indicatorAlpha)
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun NetAssetGlassCard(
    balance: BigDecimal,
    assets: BigDecimal,
    liabilities: BigDecimal,
    hideAmount: Boolean,
    onToggleHide: () -> Unit,
    onAssetManageClick: () -> Unit,
    secureMoney: (BigDecimal) -> String,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp,
) {
    NetAssetGradientCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "净资产", style = IcokieTextStyles.titleMedium, color = Color(0xFF4A3A12))
                    Icon(
                        imageVector = if (hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "切换金额可见",
                        tint = Color(0xFF6B5420),
                        modifier = Modifier.size(16.dp).clickable(onClick = onToggleHide),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                        .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onAssetManageClick)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Money,
                            contentDescription = "资产管理",
                            tint = Color(0xFF5A4620),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(text = "资产管理", style = IcokieTextStyles.labelSmall, color = Color(0xFF5A4620))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF6F5928),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HeroAmountText(
                amountText = secureMoney(balance),
                primaryColor = Color(0xFF3D2F10),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.17f))
                    .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "资产", style = IcokieTextStyles.labelSmall, color = Color(0xFF735A28))
                    Text(text = secureMoney(assets), style = IcokieTextStyles.titleMedium, color = Color(0xFF493812))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "负债", style = IcokieTextStyles.labelSmall, color = Color(0xFF735A28))
                    Text(text = secureMoney(liabilities), style = IcokieTextStyles.titleMedium, color = Color(0xFF493812))
                }
            }
        }
    }
}

@Composable
private fun MonthlyExpenseGlassCard(
    selectedMonth: LocalDate,
    income: BigDecimal,
    expense: BigDecimal,
    hideAmount: Boolean,
    onToggleHide: () -> Unit,
    onStatisticsClick: () -> Unit,
    secureMoney: (BigDecimal) -> String,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp,
) {
    val monthlyBalance = income - expense

    MonthlyExpenseGradientCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${selectedMonth.monthValue}月支出", style = IcokieTextStyles.titleMedium, color = Color(0xFFF2F6FF))
                    Icon(
                        imageVector = if (hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "切换金额可见",
                        tint = Color(0xFFE2ECFF),
                        modifier = Modifier.size(16.dp).clickable(onClick = onToggleHide),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.19f))
                        .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onStatisticsClick)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "统计",
                            tint = Color(0xFFF5F9FF),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(text = "统计", style = IcokieTextStyles.labelSmall, color = Color(0xFFF5F9FF))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFEAF2FF),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HeroAmountText(
                amountText = secureMoney(expense),
                primaryColor = Color(0xFFF9FCFF),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "收入", style = IcokieTextStyles.labelSmall, color = Color(0xFFE4ECFF))
                    Text(text = secureMoney(income), style = IcokieTextStyles.titleMedium, color = Color(0xFFFCFEFF))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "结余", style = IcokieTextStyles.labelSmall, color = Color(0xFFE4ECFF))
                    Text(text = secureMoney(monthlyBalance), style = IcokieTextStyles.titleMedium, color = Color(0xFFFCFEFF))
                }
            }
        }
    }
}

@Composable
private fun HeroAmountText(
    amountText: String,
    primaryColor: Color,
    modifier: Modifier = Modifier,
) {
    if (!amountText.startsWith("¥") || !amountText.contains(".")) {
        Text(
            text = amountText,
            style = IcokieTextStyles.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = primaryColor,
            modifier = modifier,
        )
        return
    }

    val numberPart = amountText.removePrefix("¥")
    val integerPart = numberPart.substringBefore(".")
    val decimalPart = numberPart.substringAfter(".", "00")

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "¥",
            style = IcokieTextStyles.titleLarge,
            color = primaryColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Text(
            text = integerPart,
            style = IcokieTextStyles.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = primaryColor,
        )
        Text(
            text = ".$decimalPart",
            style = IcokieTextStyles.titleLarge,
            color = primaryColor.copy(alpha = 0.68f),
            modifier = Modifier.padding(start = 1.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun QuickActions(
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "记一笔",
                style = IcokieTextStyles.labelMedium
            )
        }

        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OnSurfacePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "导入账单",
                style = IcokieTextStyles.labelMedium
            )
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    income: BigDecimal,
    expense: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing
    val elevation = IcokieTheme.elevation

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
        ) {
            Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = SurfacePrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(IncomeDefault)
                    )
                    Text(
                        text = "本月收入",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.itemSpacing))
                Text(
                    text = "+$currency ${income.toPlainString()}",
                    style = IcokieTextStyles.titleLarge,
                    color = IncomeDefault
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = SurfacePrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ExpenseDefault)
                    )
                    Text(
                        text = "本月支出",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.itemSpacing))
                Text(
                    text = "-$currency ${expense.toPlainString()}",
                    style = IcokieTextStyles.titleLarge,
                    color = ExpenseDefault
                )
            }
        }
    }
}

@Composable
private fun DaySectionHeader(
    date: LocalDate,
    dayTransactions: List<Transaction>,
    modifier: Modifier = Modifier,
) {
    val expense = dayTransactions
        .filter { it.type == TransactionType.EXPENSE && !it.isPending }
        .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    val income = dayTransactions
        .filter { it.type == TransactionType.INCOME && !it.isPending }
        .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }

    val isToday = date == AppDateTime.today()
    val datePart = date.format(DateTimeFormatter.ofPattern("M月d日（E）", Locale.CHINA))
    val title = if (isToday) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = OnSurfacePrimary)) {
                append("今天")
            }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary.copy(alpha = 0.9f))) {
                append(datePart)
            }
        }
    } else {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary)) {
                append(datePart)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = IcokieTextStyles.titleMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "支出¥${expense.pretty()} | 收入¥${income.pretty()}",
            style = IcokieTextStyles.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
            ),
            color = OnSurfaceSecondary.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun TimelineTransactionRecord(
    transaction: Transaction,
    accountName: String?,
    modifier: Modifier = Modifier,
) {
    val visual = transaction.resolveCategoryVisual()
    val remark = transaction.description?.takeIf { it.isNotBlank() } ?: transaction.title

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = transaction.time.toClockText(),
            style = IcokieTextStyles.labelMedium,
            color = OnSurfaceSecondary,
            modifier = Modifier
                .width(42.dp)
                .padding(top = 2.dp),
        )

        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFD5DEEC)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(visual.amountColor.copy(alpha = 0.45f)),
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(visual.chipBackground)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = visual.label, style = IcokieTextStyles.labelMedium, color = visual.chipText)
                    Text(text = visual.emoji)
                }

                Text(
                    text = transaction.prettyAmount(),
                    style = IcokieTextStyles.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = visual.amountColor,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F4F8))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = remark,
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = accountName ?: "未选择账户",
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "系统定位",
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }
        }
    }
}

private data class TimelineCategoryVisual(
    val label: String,
    val emoji: String,
    val chipBackground: Color,
    val chipText: Color,
    val amountColor: Color,
)

private val CategoryFoodId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")
private val CategoryShoppingId = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")
private val CategoryTransportId = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333")
private val CategoryIncomeId = java.util.UUID.fromString("44444444-4444-4444-4444-444444444444")

private fun Transaction.resolveCategoryVisual(): TimelineCategoryVisual {
    return when {
        categoryId == CategoryFoodId -> TimelineCategoryVisual(
            label = "餐饮",
            emoji = "🍜",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryShoppingId -> TimelineCategoryVisual(
            label = "购物",
            emoji = "🛍️",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryTransportId -> TimelineCategoryVisual(
            label = "交通",
            emoji = "🚗",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryIncomeId || type == TransactionType.INCOME -> TimelineCategoryVisual(
            label = "收入",
            emoji = "📦",
            chipBackground = Color(0xFFFCE8D4),
            chipText = Color(0xFFB85B06),
            amountColor = Color(0xFFB85B06),
        )

        type == TransactionType.TRANSFER -> TimelineCategoryVisual(
            label = "转账",
            emoji = "💸",
            chipBackground = Color(0xFFE9EEF8),
            chipText = Color(0xFF54627A),
            amountColor = Color(0xFF2B3345),
        )

        else -> TimelineCategoryVisual(
            label = "支出",
            emoji = "🧾",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )
    }
}

private fun Transaction.prettyAmount(): String {
    val value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    return when (type) {
        TransactionType.INCOME -> "+¥$value"
        TransactionType.EXPENSE -> "-¥$value"
        TransactionType.TRANSFER -> "¥$value"
    }
}

private fun BigDecimal.pretty(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun java.time.Instant.toClockText(): String {
    return atZone(AppDateTime.zoneId)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun EmptyRecentTransactions(
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sectionSpacing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = OnSurfaceTertiary
            )
            Text(
                text = "暂无交易记录",
                style = IcokieTextStyles.bodyLarge,
                color = OnSurfaceSecondary
            )
            Text(
                text = "点击「记一笔」添加第一笔交易",
                style = IcokieTextStyles.labelSmall,
                color = OnSurfaceTertiary
            )
        }
    }
}

private val InfoBackground = Color(0xFFDBEAFE)
private val InfoDefault = Color(0xFF3B82F6)
private val ExpenseBackground = Color(0xFFFEE2E2)
private val IncomeBackground = Color(0xFFD1FAE5)
private val SurfaceSecondary = Color(0xFFF8FAFC)

@Composable
private fun MonthSelector(
    modifier: Modifier = Modifier,
    selectedMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "上个月",
                tint = OnSurfacePrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${selectedMonth.monthValue}月",
                style = IcokieTextStyles.titleMedium,
                color = OnSurfacePrimary
            )
            Text(
                text = "${selectedMonth.year}年",
                style = IcokieTextStyles.labelSmall,
                color = OnSurfaceSecondary
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "下个月",
                tint = OnSurfacePrimary
            )
        }
    }
}

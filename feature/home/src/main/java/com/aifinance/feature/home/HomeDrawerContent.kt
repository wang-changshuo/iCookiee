package com.aifinance.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val DrawerBackground = Color(0xFFF4F5F7)
private val DrawerCard = Color.White
private val TileExpense = Color(0xFFA6C6FF)
private val TileWithIncome = Color(0xFF2F67DE)
private val TileEmpty = Color(0xFFF0F1F4)

@Composable
fun HomeDrawerContent(
    onNavigateToStatistics: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val transactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val currentMonth = remember { LocalDate.now().withDayOfMonth(1) }
    val monthDays = currentMonth.lengthOfMonth()
    val monthMap = remember(transactions, currentMonth) {
        buildMonthActivityMap(transactions, currentMonth)
    }

    var isLoggedIn by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .background(DrawerBackground)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DrawerCard),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF1A1A1A), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = if (isLoggedIn) "彭家城" else "未登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isLoggedIn) "已启用本地模拟登录" else "登录后可同步账单与设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93),
                        )
                    }
                    IconButton(onClick = { isLoggedIn = !isLoggedIn }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "登录管理",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFA0A3AB),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LoginChip(
                        icon = Icons.Default.Wallet,
                        text = "微信登录",
                        active = isLoggedIn,
                        onClick = { isLoggedIn = true },
                    )
                    LoginChip(
                        icon = Icons.Default.QrCode2,
                        text = "手机验证码",
                        active = isLoggedIn,
                        onClick = { isLoggedIn = true },
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DrawerCard),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${currentMonth.year}年${currentMonth.monthValue}月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(72.dp),
                        userScrollEnabled = false,
                    ) {
                        items(monthDays) { dayIndex ->
                            val day = dayIndex + 1
                            val status = monthMap[day] ?: DayActivity.None
                            val color = when (status) {
                                DayActivity.None -> TileEmpty
                                DayActivity.ExpenseOnly -> TileExpense
                                DayActivity.WithIncome -> TileWithIncome
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(color, RoundedCornerShape(5.dp))
                                    .border(
                                        width = if (day == LocalDate.now().dayOfMonth) 1.dp else 0.dp,
                                        color = if (day == LocalDate.now().dayOfMonth) TileWithIncome else Color.Transparent,
                                        shape = RoundedCornerShape(5.dp),
                                    ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val recordDays = monthMap.count { it.value != DayActivity.None }
                    val totalRecords = transactions.count { it.date.year == currentMonth.year && it.date.monthValue == currentMonth.monthValue }
                    val streak = calculateStreak(monthMap)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DrawerMetricItem("${recordDays}天", "坚持记录", Modifier.weight(1f))
                        DrawerMetricItem("${totalRecords}条", "总记录", Modifier.weight(1f))
                        DrawerMetricItem("${streak}天", "连续记录", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DrawerCard),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("常用功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DrawerFeature("图表统计", Icons.Default.BarChart) { onNavigateToStatistics() }
                        DrawerFeature("资产管理", Icons.Default.Wallet) { onNavigateToAssetManagement() }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DrawerFeature("预算管理", Icons.Default.Calculate) { }
                        DrawerFeature("分类管理", Icons.Default.Category) { }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DrawerCard),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("快捷记账", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DrawerFeature("导入导出", Icons.Default.CheckCircleOutline) { }
                        DrawerFeature("自动记账", Icons.Default.AutoAwesome) { }
                        DrawerFeature("定时记账", Icons.Default.EventRepeat) { }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DrawerCard),
                modifier = Modifier.clickable(onClick = onNavigateToSettings),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF6F727A))
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFFB1B4BC),
                    )
                }
            }
        }
    }
}

private enum class DayActivity {
    None,
    ExpenseOnly,
    WithIncome,
}

private fun buildMonthActivityMap(
    transactions: List<Transaction>,
    monthStart: LocalDate,
): Map<Int, DayActivity> {
    val monthTransactions = transactions.filter {
        it.date.year == monthStart.year && it.date.monthValue == monthStart.monthValue && !it.isPending
    }

    return monthTransactions
        .groupBy { it.date.dayOfMonth }
        .mapValues { (_, dayItems) ->
            val hasIncome = dayItems.any { it.type == TransactionType.INCOME }
            val hasExpense = dayItems.any { it.type == TransactionType.EXPENSE }
            when {
                hasIncome -> DayActivity.WithIncome
                hasExpense -> DayActivity.ExpenseOnly
                else -> DayActivity.None
            }
        }
}

private fun calculateStreak(dayMap: Map<Int, DayActivity>): Int {
    if (dayMap.isEmpty()) return 0
    val today = LocalDate.now().dayOfMonth
    var count = 0
    for (day in today downTo 1) {
        if ((dayMap[day] ?: DayActivity.None) == DayActivity.None) break
        count++
    }
    if (count > 0) return count

    val sortedDays = dayMap.keys.sortedDescending()
    val latest = sortedDays.firstOrNull() ?: return 0
    var fromLatest = 0
    for (day in latest downTo 1) {
        if ((dayMap[day] ?: DayActivity.None) == DayActivity.None) break
        fromLatest++
    }
    return fromLatest
}

@Composable
private fun LoginChip(
    icon: ImageVector,
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = if (active) Color(0xFFE8F0FF) else Color(0xFFF2F3F5),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF3A6DE0))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color(0xFF3A6DE0))
    }
}

@Composable
private fun DrawerMetricItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF999CA5))
    }
}

@Composable
private fun DrawerFeature(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 86.dp, height = 74.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF5C5F67))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3F424A))
    }
}

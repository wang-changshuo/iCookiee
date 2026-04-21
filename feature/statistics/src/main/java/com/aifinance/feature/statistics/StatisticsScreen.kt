package com.aifinance.feature.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import io.github.koalaplot.core.pie.BezierLabelConnector
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

private val ExpenseBlue = Color(0xFF2F67DE)
private val IncomeOrange = Color(0xFFFF8A00)
private val BalanceGreen = Color(0xFF4CCB93)

enum class StatisticsPeriod(val label: String) {
    WEEK("周"),
    MONTH("月"),
    YEAR("年"),
}

enum class TrendMetric(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    BALANCE("结余"),
}

enum class CompositionMode(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
}

data class AggregateStats(
    val expense: BigDecimal = BigDecimal.ZERO,
    val income: BigDecimal = BigDecimal.ZERO,
) {
    val balance: BigDecimal = income - expense
}

data class TrendBucket(
    val label: String,
    val expense: BigDecimal,
    val income: BigDecimal,
) {
    val balance: BigDecimal = income - expense
}

data class CategoryItem(
    val name: String,
    val icon: String,
    val amount: BigDecimal,
    val count: Int,
    val ratio: Float,
    val color: Color,
    val transactions: List<Transaction>,
)

data class SummaryRows(
    val total: AggregateStats,
    val average: AggregateStats,
)

data class StatisticsUiState(
    val period: StatisticsPeriod = StatisticsPeriod.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
    val aggregate: AggregateStats = AggregateStats(),
    val trendMetric: TrendMetric = TrendMetric.EXPENSE,
    val trendBuckets: List<TrendBucket> = emptyList(),
    val trendHeadline: String = "",
    val compositionMode: CompositionMode = CompositionMode.EXPENSE,
    val compositionItems: List<CategoryItem> = emptyList(),
    val rankingExpanded: Boolean = false,
    val summaryRows: SummaryRows = SummaryRows(AggregateStats(), AggregateStats()),
)

private data class StatisticsInputs(
    val transactions: List<Transaction>,
    val categories: List<com.aifinance.core.model.Category>,
    val period: StatisticsPeriod,
    val anchorDate: LocalDate,
    val trendMetric: TrendMetric,
    val compositionMode: CompositionMode,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val periodFlow = MutableStateFlow(StatisticsPeriod.MONTH)
    private val anchorDateFlow = MutableStateFlow(LocalDate.now())
    private val trendMetricFlow = MutableStateFlow(TrendMetric.EXPENSE)
    private val compositionModeFlow = MutableStateFlow(CompositionMode.EXPENSE)
    private val rankingExpandedFlow = MutableStateFlow(false)

    private val transactionsFlow = transactionRepository.getAllTransactions()
    private val categoriesFlow = categoryRepository.getAllCategories()

    private val baseInputs: StateFlow<StatisticsInputs> = combine(
        combine(transactionsFlow, categoriesFlow) { t, c -> t to c },
        periodFlow,
        anchorDateFlow,
        trendMetricFlow,
        compositionModeFlow,
    ) { transPair, period, anchorDate, trendMetric, compositionMode ->
        StatisticsInputs(
            transactions = transPair.first,
            categories = transPair.second,
            period = period,
            anchorDate = anchorDate,
            trendMetric = trendMetric,
            compositionMode = compositionMode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsInputs(
            transactions = emptyList(),
            categories = emptyList(),
            period = StatisticsPeriod.MONTH,
            anchorDate = LocalDate.now(),
            trendMetric = TrendMetric.EXPENSE,
            compositionMode = CompositionMode.EXPENSE,
        )
    )

    val uiState: StateFlow<StatisticsUiState> = combine(
        baseInputs,
        rankingExpandedFlow,
    ) { inputs, rankingExpanded ->
        val transactions = inputs.transactions
        val categories = inputs.categories
        val period = inputs.period
        val anchorDate = inputs.anchorDate
        val trendMetric = inputs.trendMetric
        val compositionMode = inputs.compositionMode

        val cleanTransactions = transactions.filter { !it.isPending }
        val filtered = cleanTransactions.filter { it.date.inPeriod(period, anchorDate) }

        val aggregate = AggregateStats(
            expense = filtered.sumAmount(TransactionType.EXPENSE),
            income = filtered.sumAmount(TransactionType.INCOME),
        )
        val trendBuckets = buildTrendBuckets(filtered, period, anchorDate)
        val compositionItems = buildCategoryItems(filtered, compositionMode, categories)
        val divisor = period.divisor(anchorDate)
        val summaryRows = SummaryRows(
            total = aggregate,
            average = AggregateStats(
                expense = aggregate.expense.divide(BigDecimal(divisor), 2, RoundingMode.HALF_UP),
                income = aggregate.income.divide(BigDecimal(divisor), 2, RoundingMode.HALF_UP),
            ),
        )
        val trendHeadline = buildTrendHeadline(trendBuckets, trendMetric)

        StatisticsUiState(
            period = period,
            anchorDate = anchorDate,
            aggregate = aggregate,
            trendMetric = trendMetric,
            trendBuckets = trendBuckets,
            trendHeadline = trendHeadline,
            compositionMode = compositionMode,
            compositionItems = compositionItems,
            rankingExpanded = rankingExpanded,
            summaryRows = summaryRows,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState(),
    )

    fun selectPeriod(period: StatisticsPeriod) {
        periodFlow.value = period
        anchorDateFlow.value = LocalDate.now()
    }

    fun previousPeriod() {
        anchorDateFlow.value = when (periodFlow.value) {
            StatisticsPeriod.WEEK -> anchorDateFlow.value.minusWeeks(1)
            StatisticsPeriod.MONTH -> anchorDateFlow.value.minusMonths(1)
            StatisticsPeriod.YEAR -> anchorDateFlow.value.minusYears(1)
        }
    }

    fun nextPeriod() {
        anchorDateFlow.value = when (periodFlow.value) {
            StatisticsPeriod.WEEK -> anchorDateFlow.value.plusWeeks(1)
            StatisticsPeriod.MONTH -> anchorDateFlow.value.plusMonths(1)
            StatisticsPeriod.YEAR -> anchorDateFlow.value.plusYears(1)
        }
    }

    fun selectTrendMetric(metric: TrendMetric) {
        trendMetricFlow.value = metric
    }

    fun selectComposition(mode: CompositionMode) {
        compositionModeFlow.value = mode
    }

    fun toggleRanking() {
        rankingExpandedFlow.value = !rankingExpandedFlow.value
    }
}

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFF4FD)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatisticsHeader(onBack = onBack)
        }

        item {
            PeriodSwitchCard(
                period = uiState.period,
                onSelect = viewModel::selectPeriod,
            )
        }

        item {
            PeriodAnchorBar(
                period = uiState.period,
                anchorDate = uiState.anchorDate,
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod,
            )
        }

        item {
            AggregateCard(uiState.aggregate)
        }

        item {
            AiPlaceholderCard()
        }

        item {
            TrendCard(
                trendMetric = uiState.trendMetric,
                trendBuckets = uiState.trendBuckets,
                trendHeadline = uiState.trendHeadline,
                onMetricSelect = viewModel::selectTrendMetric,
            )
        }

        item {
            CompositionCard(
                mode = uiState.compositionMode,
                items = uiState.compositionItems,
                rankingExpanded = uiState.rankingExpanded,
                onModeSelect = viewModel::selectComposition,
                onToggleRanking = viewModel::toggleRanking,
            )
        }

        item {
            SummaryCard(rows = uiState.summaryRows)
        }
    }
}

@Composable
private fun StatisticsHeader(onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onBack),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "i",
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.8.sp),
                    color = IncomeOrange,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Cookie",
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.4.sp),
                    color = ExpenseBlue,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Spacer(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun PeriodSwitchCard(
    period: StatisticsPeriod,
    onSelect: (StatisticsPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD4E8F8), RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatisticsPeriod.entries.forEach { item ->
            val selected = item == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { onSelect(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.label, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1F2937))
            }
        }
    }
}

@Composable
private fun PeriodAnchorBar(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val title = when (period) {
        StatisticsPeriod.WEEK -> {
            val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            "${start.year}.${start.monthValue}.${start.dayOfMonth} - ${end.monthValue}.${end.dayOfMonth}"
        }

        StatisticsPeriod.MONTH -> "${anchorDate.year}年${anchorDate.monthValue}月"
        StatisticsPeriod.YEAR -> "${anchorDate.year}年"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一周期", modifier = Modifier.clickable(onClick = onPrevious))
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周期", modifier = Modifier.clickable(onClick = onNext))
    }
}

@Composable
private fun AggregateCard(aggregate: AggregateStats) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AggregateItem("支出", aggregate.expense, ExpenseBlue)
            AggregateItem("收入", aggregate.income, IncomeOrange)
            AggregateItem("结余", aggregate.balance, BalanceGreen)
        }
    }
}

@Composable
private fun AggregateItem(label: String, value: BigDecimal, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
        val moneyText = prettyMoneyWithSign(value)
        // 动态调整字体大小：数字越长，字体越小
        val fontSize = when {
            moneyText.length > 14 -> 12.sp
            moneyText.length > 12 -> 14.sp
            moneyText.length > 10 -> 16.sp
            else -> 18.sp
        }
        Text(
            text = moneyText,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AiPlaceholderCard() {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "AI分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "AI分析功能后续接入，这里预留分析卡片位。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
            )
        }
    }
}

@Composable
private fun TrendCard(
    trendMetric: TrendMetric,
    trendBuckets: List<TrendBucket>,
    trendHeadline: String,
    onMetricSelect: (TrendMetric) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (trendMetric) {
                        TrendMetric.EXPENSE -> "每日支出趋势"
                        TrendMetric.INCOME -> "每日收入趋势"
                        TrendMetric.BALANCE -> "每日结余趋势"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                SegmentedToggle(
                    labels = TrendMetric.entries.map { it.label },
                    selectedLabel = trendMetric.label,
                    onSelect = { selected ->
                        onMetricSelect(TrendMetric.entries.first { it.label == selected })
                    },
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = trendHeadline, style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
            }

            TrendChart(
                metric = trendMetric,
                buckets = trendBuckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            AxisLabels(labels = trendBuckets.axisLabels())
        }
    }
}

@Composable
private fun TrendChart(
    metric: TrendMetric,
    buckets: List<TrendBucket>,
    modifier: Modifier = Modifier,
) {
    val values = buckets.map {
        when (metric) {
            TrendMetric.EXPENSE -> it.expense.toFloatSafe()
            TrendMetric.INCOME -> it.income.toFloatSafe()
            TrendMetric.BALANCE -> it.balance.toFloatSafe()
        }
    }
    val maxValue = max(values.maxOrNull() ?: 0f, 1f)
    val minValue = if (metric == TrendMetric.BALANCE) minOf(values.minOrNull() ?: 0f, 0f) else 0f
    val range = max(maxValue - minValue, 1f)

    Canvas(modifier = modifier) {
        val startX = 8.dp.toPx()
        val endX = size.width - 8.dp.toPx()
        val topY = 12.dp.toPx()
        val bottomY = size.height - 20.dp.toPx()
        val barAreaWidth = endX - startX
        val step = if (values.isNotEmpty()) barAreaWidth / values.size else barAreaWidth
        val barWidth = (step * 0.42f).coerceAtLeast(4.dp.toPx())
        val zeroY = bottomY - ((0f - minValue) / range) * (bottomY - topY)

        drawLine(
            color = Color(0xFFD1D5DB),
            start = Offset(startX, zeroY),
            end = Offset(endX, zeroY),
            strokeWidth = 1.dp.toPx(),
        )

        values.forEachIndexed { index, value ->
            val x = startX + step * index + step * 0.29f
            val y = bottomY - ((value - minValue) / range) * (bottomY - topY)
            val top = minOf(y, zeroY)
            val height = kotlin.math.abs(y - zeroY)
            val color = when (metric) {
                TrendMetric.EXPENSE -> ExpenseBlue
                TrendMetric.INCOME -> IncomeOrange
                TrendMetric.BALANCE -> if (value >= 0f) BalanceGreen else Color(0xFF9CA3AF)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, top),
                size = Size(barWidth, max(height, 2.dp.toPx())),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
        }

        if (metric == TrendMetric.BALANCE && values.size > 1) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = startX + step * index + step * 0.5f
                val y = bottomY - ((value - minValue) / range) * (bottomY - topY)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path = path, color = BalanceGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    if (labels.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CompositionCard(
    mode: CompositionMode,
    items: List<CategoryItem>,
    rankingExpanded: Boolean,
    onModeSelect: (CompositionMode) -> Unit,
    onToggleRanking: () -> Unit,
) {
    var expandedCategory by rememberSaveable(mode) { mutableStateOf<String?>(null) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (mode == CompositionMode.EXPENSE) "支出分类构成" else "收入分类构成",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                SegmentedToggle(
                    labels = CompositionMode.entries.map { it.label },
                    selectedLabel = mode.label,
                    onSelect = { selected ->
                        onModeSelect(CompositionMode.entries.first { it.label == selected })
                    },
                )
            }

            DonutChart(items = items, centerTitle = if (mode == CompositionMode.EXPENSE) "本期支出" else "本期收入")

            val shown = if (rankingExpanded) items else items.take(5)
            shown.forEach { item ->
                CategoryRow(
                    item = item,
                    expanded = expandedCategory == item.name,
                    onToggle = {
                        expandedCategory = if (expandedCategory == item.name) null else item.name
                    },
                )
            }

            if (items.size > 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleRanking)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (rankingExpanded) "收起" else "展开全部",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF9CA3AF),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun DonutChart(items: List<CategoryItem>, centerTitle: String) {
    val total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    val values = items.map { it.amount.toFloatSafe() }
    val labelColor = Color(0xFF8B97AA)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        if (items.isEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(296.dp)
            ) {
                val center = Offset(x = size.width / 2f, y = size.height * 0.57f)
                val outerRadius = minOf(size.width * 0.23f, size.height * 0.24f)
                val ringWidth = 30.dp.toPx()
                drawArc(
                    color = Color(0xFFDDE2EE),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = ringWidth, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                )
            }
        } else {
            PieChart(
                values = values,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(296.dp),
                holeSize = 0.68f,
                labelSpacing = 1.20f,
                forceCenteredPie = true,
                slice = { index ->
                    DefaultSlice(
                        color = items[index].color,
                        gap = if (items.size > 1) 1.4f else 0f,
                        antiAlias = true,
                    )
                },
                label = { index ->
                    val item = items[index]
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF1F2937),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${(item.ratio * 100f).prettyPercent()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor,
                        )
                    }
                },
                labelConnector = {
                    BezierLabelConnector(
                        connectorColor = Color(0xFF98A6BF),
                        connectorStroke = Stroke(width = 1.6f),
                    )
                },
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = centerTitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            Text(text = "¥${total.pretty()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryRow(
    item: CategoryItem,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCFDFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = item.icon)
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${(item.ratio * 100f).prettyPercent()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B97AA),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "¥${item.amount.pretty()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val chevronRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing),
                    label = "categoryChevronRotation",
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.ratio.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(item.color, RoundedCornerShape(10.dp)),
            )
        }
        Text(text = "${item.count}笔", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF), modifier = Modifier.align(Alignment.End))

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(280, easing = LinearOutSlowInEasing)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F8FC), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.transactions.take(5).forEachIndexed { index, transaction ->
                    TransactionDetailRow(transaction = transaction, accentColor = item.color)
                    if (index != minOf(item.transactions.size, 5) - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE6EBF5)),
                        )
                    }
                }
                if (item.transactions.size > 5) {
                    Text(
                        text = "仅展示最近5笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B97AA),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(
    transaction: Transaction,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1F2937),
                )
                Text(
                    text = transaction.date.toDisplayLabel() + " " + transaction.time.toTimeLabel() + (transaction.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF8B97AA),
                )
            }
        }
        Text(
            text = prettyMoneyWithSign(if (transaction.type == TransactionType.EXPENSE) transaction.amount.negate() else transaction.amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == TransactionType.EXPENSE) ExpenseBlue else IncomeOrange,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SummaryCard(rows: SummaryRows) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "账单汇总", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFF6F8FF), Color(0xFFF1F4FC)),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryHeaderCell("类型", modifier = Modifier.weight(1.0f), textAlign = TextAlign.Start)
                    SummaryHeaderCell("支出", modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                    SummaryHeaderCell("收入", modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                    SummaryHeaderCell("结余", modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                }

                SummaryDataRow("日均", rows.average, false)
            }
        }
    }
}

@Composable
private fun SummaryHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign,
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF667085),
    )
}

@Composable
private fun SummaryDataRow(
    title: String,
    stats: AggregateStats,
    emphasize: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1.0f),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937),
        )
        SummaryAmountCell(value = stats.expense, color = ExpenseBlue, emphasize = emphasize)
        SummaryAmountCell(value = stats.income, color = IncomeOrange, emphasize = emphasize)
        SummaryAmountCell(value = stats.balance, color = BalanceGreen, emphasize = emphasize)
    }
}

@Composable
private fun RowScope.SummaryAmountCell(
    value: BigDecimal,
    color: Color,
    emphasize: Boolean,
) {
    val text = prettyMoneyWithSign(value)
    // 动态计算字体大小，避免换行
    val maxChars = if (emphasize) 10 else 11
    val fontSize = when {
        text.length > maxChars + 4 -> 12.sp
        text.length > maxChars + 2 -> 13.sp
        text.length > maxChars -> 14.sp
        else -> if (emphasize) 16.sp else 14.sp
    }
    Text(
        text = text,
        modifier = Modifier.weight(1.3f),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFDDE3F0)),
    )
}

@Composable
private fun SegmentedToggle(
    labels: List<String>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { label ->
            val selected = label == selectedLabel
            Box(
                modifier = Modifier
                    .background(if (selected) ExpenseBlue else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { onSelect(label) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else Color(0xFF374151),
                )
            }
        }
    }
}

private fun List<TrendBucket>.axisLabels(): List<String> {
    if (isEmpty()) return emptyList()
    return listOf(first().label, get(size / 2).label, last().label)
}

private fun List<Transaction>.sumAmount(type: TransactionType): BigDecimal =
    filter { it.type == type }
        .fold(BigDecimal.ZERO) { sum, transaction -> sum + transaction.amount }

private fun LocalDate.inPeriod(period: StatisticsPeriod, anchor: LocalDate): Boolean {
    return when (period) {
        StatisticsPeriod.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            !isBefore(start) && !isAfter(end)
        }

        StatisticsPeriod.MONTH -> year == anchor.year && monthValue == anchor.monthValue
        StatisticsPeriod.YEAR -> year == anchor.year
    }
}

private fun StatisticsPeriod.divisor(anchorDate: LocalDate): Int {
    return when (this) {
        StatisticsPeriod.WEEK -> 7
        StatisticsPeriod.MONTH -> YearMonth.of(anchorDate.year, anchorDate.monthValue).lengthOfMonth()
        StatisticsPeriod.YEAR -> 12
    }
}

private fun buildTrendBuckets(
    filtered: List<Transaction>,
    period: StatisticsPeriod,
    anchorDate: LocalDate,
): List<TrendBucket> {
    return when (period) {
        StatisticsPeriod.WEEK -> {
            val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0..6).map { index ->
                val day = start.plusDays(index.toLong())
                val dayTransactions = filtered.filter { it.date == day }
                TrendBucket(
                    label = "${day.monthValue}/${day.dayOfMonth}",
                    expense = dayTransactions.sumAmount(TransactionType.EXPENSE),
                    income = dayTransactions.sumAmount(TransactionType.INCOME),
                )
            }
        }

        StatisticsPeriod.MONTH -> {
            val ym = YearMonth.of(anchorDate.year, anchorDate.monthValue)
            (1..ym.lengthOfMonth()).map { day ->
                val dayTransactions = filtered.filter { it.date.dayOfMonth == day }
                TrendBucket(
                    label = "${day}日",
                    expense = dayTransactions.sumAmount(TransactionType.EXPENSE),
                    income = dayTransactions.sumAmount(TransactionType.INCOME),
                )
            }
        }

        StatisticsPeriod.YEAR -> {
            (1..12).map { month ->
                val monthTransactions = filtered.filter { it.date.monthValue == month }
                TrendBucket(
                    label = "${month}月",
                    expense = monthTransactions.sumAmount(TransactionType.EXPENSE),
                    income = monthTransactions.sumAmount(TransactionType.INCOME),
                )
            }
        }
    }
}

private fun buildCategoryItems(
    filtered: List<Transaction>,
    mode: CompositionMode,
    customCategories: List<com.aifinance.core.model.Category>,
): List<CategoryItem> {
    val targetType = if (mode == CompositionMode.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
    val target = filtered.filter { it.type == targetType }
    val total = target.fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
    if (target.isEmpty() || total == BigDecimal.ZERO) return emptyList()

    val grouped = target.groupBy { it.toCategoryKey(mode, customCategories) }
    val palette = if (mode == CompositionMode.EXPENSE) {
        listOf(Color(0xFF2F67DE), Color(0xFFFF8A00), Color(0xFF4CCB93), Color(0xFF8B5CF6), Color(0xFFF04452), Color(0xFF10B8D4))
    } else {
        listOf(Color(0xFF4CCB93), Color(0xFFFF8A00), Color(0xFF2F67DE), Color(0xFF8B5CF6), Color(0xFF10B8D4), Color(0xFFF59E0B))
    }

    val sortedEntries = grouped.entries
        .map { entry ->
            val amount = entry.value.fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
            val ratio = if (total == BigDecimal.ZERO) 0f else amount.divide(total, 4, RoundingMode.HALF_UP).toFloatSafe()
            val visual = entry.key.toVisual(mode, customCategories)
            CategoryGroup(
                visual = visual,
                amount = amount,
                count = entry.value.size,
                transactions = entry.value.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.time }),
            ) to ratio
        }
        .sortedByDescending { it.first.amount }

    return sortedEntries
        .mapIndexed { index, pair ->
            val visual = pair.first.visual
            val amount = pair.first.amount
            val count = pair.first.count
            val ratio = pair.second
            CategoryItem(
                name = visual.name,
                icon = visual.icon,
                amount = amount,
                count = count,
                ratio = ratio,
                color = palette[index % palette.size],
                transactions = pair.first.transactions,
            )
        }
}

private data class CategoryVisual(val name: String, val icon: String)

private data class CategoryGroup(
    val visual: CategoryVisual,
    val amount: BigDecimal,
    val count: Int,
    val transactions: List<Transaction>,
)

private fun Transaction.toCategoryKey(mode: CompositionMode, customCategories: List<com.aifinance.core.model.Category>): String {
    val catalogCategory = categoryId?.let { CategoryCatalog.findById(it) }
    if (catalogCategory != null) {
        return catalogCategory.name
    }
    val customCategory = categoryId?.let { id ->
        customCategories.firstOrNull { it.id == id }
    }
    if (customCategory != null) {
        return customCategory.name
    }
    val text = title + " " + (description ?: "")
    return when {
        mode == CompositionMode.INCOME && text.contains("工资") -> "工资"
        mode == CompositionMode.INCOME && text.contains("奖金") -> "奖金"
        mode == CompositionMode.INCOME -> "其他收入"
        text.contains("餐") || text.contains("外卖") -> "餐饮"
        text.contains("购") || text.contains("超市") -> "购物"
        text.contains("房") || text.contains("租") -> "住房"
        text.contains("交通") || text.contains("地铁") || text.contains("打车") -> "交通"
        text.contains("充") || text.contains("话费") -> "充值"
        else -> "其他支出"
    }
}

private fun String.toVisual(mode: CompositionMode, customCategories: List<com.aifinance.core.model.Category>): CategoryVisual {
    val catalogCategory = CategoryCatalog.all.firstOrNull { it.name == this }
    if (catalogCategory != null) {
        return CategoryVisual(catalogCategory.name, catalogCategory.icon)
    }
    val customCategory = customCategories.firstOrNull { it.name == this }
    if (customCategory != null) {
        return CategoryVisual(customCategory.name, customCategory.icon)
    }
    return when (this) {
        "餐饮" -> CategoryVisual("餐饮", "🍜")
        "购物" -> CategoryVisual("购物", "🛒")
        "住房" -> CategoryVisual("住房", "🏠")
        "交通" -> CategoryVisual("交通", "🚗")
        "通讯" -> CategoryVisual("通讯", "📱")
        "医疗" -> CategoryVisual("医疗", "💊")
        "教育" -> CategoryVisual("教育", "📚")
        "娱乐" -> CategoryVisual("娱乐", "🎮")
        "工资" -> CategoryVisual("工资", "💼")
        "奖金" -> CategoryVisual("奖金", "🎁")
        "其他收入" -> CategoryVisual("其他收入", "📦")
        else -> CategoryVisual(
            if (mode == CompositionMode.EXPENSE) "其他支出" else "其他收入",
            "📦"
        )
    }
}

private fun buildTrendHeadline(
    buckets: List<TrendBucket>,
    metric: TrendMetric,
): String {
    if (buckets.isEmpty()) return "暂无数据"
    val index = buckets.indexOfLast {
        when (metric) {
            TrendMetric.EXPENSE -> it.expense > BigDecimal.ZERO
            TrendMetric.INCOME -> it.income > BigDecimal.ZERO
            TrendMetric.BALANCE -> it.balance != BigDecimal.ZERO
        }
    }.takeIf { it >= 0 } ?: 0
    val bucket = buckets[index]
    return when (metric) {
        TrendMetric.EXPENSE -> "${bucket.label} 支出¥${bucket.expense.pretty()}"
        TrendMetric.INCOME -> "${bucket.label} 收入¥${bucket.income.pretty()}"
        TrendMetric.BALANCE -> "${bucket.label} 结余¥${bucket.balance.pretty()}"
    }
}

private fun BigDecimal.pretty(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun prettyMoneyWithSign(value: BigDecimal): String {
    val sign = if (value >= BigDecimal.ZERO) "+" else "-"
    return "$sign¥${value.abs().setScale(2, RoundingMode.HALF_UP)}"
}

private fun BigDecimal.toFloatSafe(): Float = try {
    toFloat()
} catch (_: NumberFormatException) {
    0f
}

private fun LocalDate.toDisplayLabel(): String = "${monthValue}月${dayOfMonth}日"

private fun java.time.Instant.toTimeLabel(): String {
    val localTime = atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return String.format("%02d:%02d", localTime.hour, localTime.minute)
}

private fun Float.prettyPercent(): String = String.format("%.2f", this)

package com.aifinance.feature.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.designsystem.theme.BorderSubtle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.ExpenseAccent
import com.aifinance.core.designsystem.theme.ExpenseBackground
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.Category
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.TransactionType
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val recurrencePickerOrder: List<ScheduledRecurrence> = listOf(
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

internal fun recurrenceLabel(r: ScheduledRecurrence): String = when (r) {
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

private val chineseDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA)

internal fun formatChineseDate(date: LocalDate): String = date.format(chineseDateFormatter)

internal fun scheduledEndSummary(rule: ScheduledRule): String = when (rule.endMode) {
    ScheduledEndMode.NEVER -> "不结束"
    ScheduledEndMode.END_DATE -> {
        val d = rule.endDate
        if (d != null) "${formatChineseDate(d)}止" else ""
    }
    ScheduledEndMode.AFTER_COUNT -> {
        val m = rule.maxOccurrences
        if (m != null) "共${m}笔" else ""
    }
}

internal fun nextRunDateOnlyText(rule: ScheduledRule): String {
    val n = rule.nextRunAt ?: return "—"
    val z = ZoneId.systemDefault()
    val date = LocalDateTime.ofInstant(n, z).toLocalDate()
    return formatChineseDate(date)
}

@Composable
internal fun ScheduledRuleListCard(
    rule: ScheduledRule,
    category: Category?,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
) {
    val catName = category?.name ?: "未分类"
    val catIcon = category?.icon ?: "\u2753"
    val catColor = category?.let { Color(it.color) } ?: SurfaceSecondary
    val amountStr = rule.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    val amountColor = when (rule.transactionType) {
        TransactionType.EXPENSE -> ExpenseAccent
        TransactionType.INCOME -> IncomeDefault
        else -> BrandPrimary
    }
    val amountPrefix = when (rule.transactionType) {
        TransactionType.EXPENSE -> "-¥ "
        TransactionType.INCOME -> "+¥ "
        else -> "¥ "
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = catIcon, fontSize = 22.sp)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = catName,
                            style = IcokieTextStyles.titleMedium,
                            color = OnSurfacePrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Box(
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = recurrenceLabel(rule.recurrence),
                                style = IcokieTextStyles.labelSmall,
                                color = OnSurfaceSecondary,
                            )
                        }
                    }
                    Text(
                        text = "$amountPrefix$amountStr",
                        style = IcokieTextStyles.headlineMedium.copy(fontSize = 20.sp),
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${formatChineseDate(rule.startDate)} - ${scheduledEndSummary(rule)}",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceTertiary,
                        fontWeight = FontWeight.Normal,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFEF4444),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderSubtle,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已执行 ${rule.firedCount} 笔",
                    style = IcokieTextStyles.labelSmall,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "下一笔：${nextRunDateOnlyText(rule)}",
                    style = IcokieTextStyles.labelSmall,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.weight(1.2f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (rule.enabled) BrandPrimary.copy(alpha = 0.14f) else SurfaceSecondary,
                        )
                        .clickable(onClick = onToggleEnabled)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (rule.enabled) "执行中" else "已暂停",
                        style = IcokieTextStyles.labelSmall,
                        color = if (rule.enabled) BrandPrimary else OnSurfaceTertiary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScheduledTypeButton(
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
internal fun ScheduledCategoryIconWithLabel(
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
internal fun ScheduledInfoChip(
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
internal fun EndModePill(
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

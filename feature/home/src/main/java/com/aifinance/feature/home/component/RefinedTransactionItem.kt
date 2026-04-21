package com.aifinance.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RefinedTransactionItem(
    transaction: Transaction,
    accountName: String?,
    category: Category? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    onAmountClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val visual = transaction.resolveRefinedVisual(category)
    val remark = transaction.description?.takeIf { it.isNotBlank() } ?: transaction.title
    val rowInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(visual.itemBackground, RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = transaction.time.toClockText(),
                style = IcokieTextStyles.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = OnSurfaceSecondary.copy(alpha = 0.86f),
            )

            Text(
                text = transaction.prettyAmount(),
                modifier = Modifier.clickable(
                    onClick = onAmountClick,
                ),
                style = IcokieTextStyles.titleLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = 0.sp,
                ),
                color = visual.amountColor,
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Row(
            modifier = Modifier
                .background(visual.chipBackground, RoundedCornerShape(999.dp))
                .clickable(onClick = onCategoryClick)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = visual.label,
                style = IcokieTextStyles.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = visual.chipText.copy(alpha = 0.92f),
            )
            Text(text = visual.emoji, style = MaterialTheme.typography.bodySmall)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = visual.chipText.copy(alpha = 0.4f),
                modifier = Modifier.size(9.dp),
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = remark,
            style = IcokieTextStyles.titleMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = OnSurfacePrimary,
        )

        Spacer(modifier = Modifier.size(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoItem(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                text = accountName ?: "微信",
                tint = OnSurfaceSecondary.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = IcokieTextStyles.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
            ),
            color = tint,
            maxLines = 1,
        )
    }
}

private data class VisualTheme(
    val label: String,
    val emoji: String,
    val itemBackground: Color,
    val chipBackground: Color,
    val chipText: Color,
    val amountColor: Color,
)

private data class TypeColors(
    val background: Color,
    val amount: Color,
)

private fun Transaction.resolveRefinedVisual(customCategory: Category? = null): VisualTheme {
    val categoryName: String
    val categoryIcon: String
    val categoryColorInt: Int

    if (customCategory != null) {
        categoryName = customCategory.name
        categoryIcon = customCategory.icon
        categoryColorInt = customCategory.color
    } else {
        val catalogCategory = CategoryCatalog.resolve(categoryId, type)
        categoryName = catalogCategory.name
        categoryIcon = catalogCategory.icon
        categoryColorInt = catalogCategory.color
    }

    val categoryColor = Color(categoryColorInt)

    val typeColors = when (type) {
        TransactionType.EXPENSE -> TypeColors(
            background = Color(0xFFE3F2FD),
            amount = Color(0xFF1976D2)
        )
        TransactionType.INCOME -> TypeColors(
            background = Color(0xFFFFF8E1),
            amount = Color(0xFFF57C00)
        )
        TransactionType.TRANSFER -> {
            val amountColor = if (title.startsWith("转出至")) {
                Color(0xFF1976D2)
            } else {
                Color(0xFFF57C00)
            }
            TypeColors(
                background = Color(0xFFE8F5E9),
                amount = amountColor
            )
        }
    }

    return VisualTheme(
        label = categoryName,
        emoji = categoryIcon,
        itemBackground = typeColors.background,
        chipBackground = categoryColor.copy(alpha = 0.12f),
        chipText = categoryColor,
        amountColor = typeColors.amount,
    )
}

private fun java.time.Instant.toClockText(): String {
    return atZone(AppDateTime.zoneId)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun Transaction.prettyAmount(): String {
    val value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    return when (type) {
        TransactionType.INCOME -> "+¥$value"
        TransactionType.EXPENSE -> "-¥$value"
        TransactionType.TRANSFER -> {
            if (title.startsWith("转出至")) {
                "-¥$value"
            } else if (title.startsWith("转入自")) {
                "+¥$value"
            } else {
                "¥$value"
            }
        }
    }
}

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    accountName: String?,
    category: Category? = null,
    onClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onAmountClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RefinedTransactionItem(
        transaction = transaction,
        accountName = accountName,
        category = category,
        modifier = modifier,
        onClick = onClick,
        onCategoryClick = onCategoryClick,
        onAmountClick = onAmountClick,
        onLongPress = onDeleteClick,
    )
}

package com.aifinance.feature.home.component

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import com.aifinance.core.model.CatalogCategory
import com.aifinance.core.model.TransactionType
import java.util.UUID

/**
 * Data class representing a category option in the picker.
 */
data class CategoryOption(
    val id: UUID,
    val name: String,
    val icon: String,
    val color: Color,
    val type: TransactionType,
)

/**
 * Converts a CatalogCategory to a CategoryOption.
 */
fun CatalogCategory.toOption(): CategoryOption {
    return CategoryOption(
        id = id,
        name = name,
        icon = icon,
        color = Color(color),
        type = type,
    )
}

fun com.aifinance.core.model.Category.toOption(): CategoryOption {
    return CategoryOption(
        id = id,
        name = name,
        icon = icon,
        color = Color(color),
        type = type,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    selectedCategoryId: UUID?,
    categories: List<CategoryOption>,
    onDismiss: () -> Unit,
    onSelect: (UUID) -> Unit,
    title: String = "选择分类",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        CategoryPickerContent(
            title = title,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onDismiss = onDismiss,
            onSelect = onSelect,
        )
    }
}

/**
 * Content portion of the category picker, can be used inside any container (sheet, dialog, etc.)
 */
@Composable
fun CategoryPickerContent(
    categories: List<CategoryOption>,
    selectedCategoryId: UUID?,
    onDismiss: () -> Unit,
    onSelect: (UUID) -> Unit,
    title: String = "选择分类",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = IcokieTextStyles.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = OnSurfacePrimary,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = SurfaceSecondary,
            thickness = 1.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(
                items = categories,
                key = { it.id.toString() },
            ) { category ->
                CategoryGridItem(
                    category = category,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onSelect(category.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * A single category item in the grid with explicit selected state treatment.
 */
@Composable
private fun CategoryGridItem(
    category: CategoryOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        category.color.copy(alpha = 0.16f)
    } else {
        category.color.copy(alpha = 0.08f)
    }

    val borderColor = if (isSelected) {
        category.color
    } else {
        Color.Transparent
    }

    val iconBackgroundColor = if (isSelected) {
        category.color
    } else {
        category.color.copy(alpha = 0.15f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.icon,
                fontSize = 24.sp,
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(category.color.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Text(
            text = category.name,
            style = IcokieTextStyles.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            ),
            color = if (isSelected) OnSurfacePrimary else OnSurfaceSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * A compact horizontal category chip that can trigger the category picker.
 * Shows the currently selected category with a subtle tap affordance.
 */
@Composable
fun CategoryPickerChip(
    category: CategoryOption?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (category == null) return
    val categoryColor = category.color

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(categoryColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = categoryColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = category.icon,
            fontSize = 16.sp,
        )
        Text(
            text = category.name,
            style = IcokieTextStyles.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = categoryColor,
        )
    }
}

/**
 * Alternative list-style category picker for scenarios where grid is not appropriate.
 */
@Composable
fun CategoryPickerList(
    categories: List<CategoryOption>,
    selectedCategoryId: UUID?,
    onSelect: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        categories.forEach { category ->
            CategoryListItem(
                category = category,
                isSelected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
            )
        }
    }
}

/**
 * A single category item for list-style picker.
 */
@Composable
private fun CategoryListItem(
    category: CategoryOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        category.color.copy(alpha = 0.12f)
    } else {
        SurfaceSecondary.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) category.color else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category.icon,
                    fontSize = 20.sp,
                )
            }

            Text(
                text = category.name,
                style = IcokieTextStyles.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                ),
                color = OnSurfacePrimary,
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = category.color,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun CategoryPickerLauncher(
    selectedCategoryId: UUID?,
    categories: List<CategoryOption>,
    onCategorySelected: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    chipLabel: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }

    val category = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId } ?: categories.firstOrNull()
    } ?: return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(category.color.copy(alpha = 0.12f))
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = category.icon, fontSize = 16.sp)
        Text(
            text = chipLabel ?: category.name,
            style = IcokieTextStyles.labelMedium,
            color = category.color,
        )
    }

    if (showPicker) {
        CategoryPickerBottomSheet(
            selectedCategoryId = selectedCategoryId,
            categories = categories,
            onDismiss = { showPicker = false },
            onSelect = { categoryId ->
                onCategorySelected(categoryId)
                showPicker = false
            },
        )
    }
}

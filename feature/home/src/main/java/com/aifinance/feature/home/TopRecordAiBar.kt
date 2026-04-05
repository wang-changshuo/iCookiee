package com.aifinance.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val TabSurface = Color(0xFFEFF3FB)
private val TabIndicator = BrandPrimary

@Composable
fun TopRecordAiBar(
    selectedTab: HomeTopTab,
    onMenuClick: () -> Unit,
    onTabSelected: (HomeTopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "菜单",
                tint = OnSurfacePrimary,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            liquidTopTabBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 276.dp),
            )
        }
    }
}

@Composable
private fun liquidTopTabBar(
    selectedTab: HomeTopTab,
    onTabSelected: (HomeTopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(HomeTopTab.RECORD to "记录", HomeTopTab.AI_ASSISTANT to "iCookie")
    val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

    val leadingBlob by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 520f,
        ),
        label = "liquid_tab_leading_blob",
    )
    val trailingBlob by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 260f,
        ),
        label = "liquid_tab_trailing_blob",
    )

    Box(
        modifier = Modifier
            .then(modifier)
            .clip(RoundedCornerShape(24.dp))
            .background(TabSurface)
            .padding(4.dp)
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val slotWidth = size.width / tabs.size
            val centerY = size.height / 2f
            val leadX = (leadingBlob + 0.5f) * slotWidth
            val trailX = (trailingBlob + 0.5f) * slotWidth

            val capsuleHeight = size.height * 0.88f
            val capsuleWidth = slotWidth * 0.72f
            val corner = capsuleHeight / 2f
            val leadLeft = leadX - capsuleWidth / 2f
            val trailLeft = trailX - capsuleWidth / 2f
            val minLeft = min(leadLeft, trailLeft)
            val maxLeft = max(leadLeft, trailLeft)
            val spread = abs(maxLeft - minLeft)
            val capsuleTop = centerY - capsuleHeight / 2f
            val stretchedWidth = capsuleWidth + spread

            drawRoundRect(
                color = TabIndicator,
                topLeft = Offset(minLeft, capsuleTop),
                size = Size(stretchedWidth, capsuleHeight),
                cornerRadius = CornerRadius(corner, corner),
            )

            val glossWidth = capsuleWidth * 0.34f
            val glossHeight = capsuleHeight * 0.24f
            val glossX = leadLeft + capsuleWidth * 0.14f
            val glossY = capsuleTop + capsuleHeight * 0.14f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(glossX, glossY),
                size = Size(glossWidth, glossHeight),
                cornerRadius = CornerRadius(glossHeight / 2f, glossHeight / 2f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else OnSurfacePrimary,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 480f,
                    ),
                    label = "liquid_tab_text_color_$index",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .selectable(
                            selected = selected,
                            onClick = { onTabSelected(tab.first) },
                            role = Role.Tab,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.second,
                        style = if (selected) {
                            IcokieTextStyles.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.05.sp,
                            )
                        } else {
                            IcokieTextStyles.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.8.sp,
                            )
                        },
                        color = textColor,
                    )
                }
            }
        }
    }
}

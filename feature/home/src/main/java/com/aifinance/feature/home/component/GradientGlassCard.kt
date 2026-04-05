package com.aifinance.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 彩色渐变玻璃卡片组件
 * 
 * 适用于金色、蓝色等彩色渐变背景，配合玻璃拟态效果
 * 
 * @param modifier Modifier
 * @param cornerRadius 圆角大小
 * @param gradientColors 渐变颜色列表（至少2个）
 * @param gradientStart 渐变起点
 * @param gradientEnd 渐变终点
 * @param highlightColor 高光颜色（通常是白色）
 * @param highlightAlpha 高光透明度
 * @param borderColor 边框颜色
 * @param contentAlignment 内容对齐方式
 * @param content 内容
 */
@Composable
fun GradientGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    gradientColors: List<Color>,
    gradientStart: Offset = Offset(0f, 0f),
    gradientEnd: Offset = Offset(980f, 720f),
    highlightColor: Color = Color.White,
    highlightAlpha: Float = 0.45f,
    borderColor: Color = Color.White.copy(alpha = 0.34f),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val cardShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = gradientStart,
                    end = gradientEnd,
                )
            )
            .border(1.dp, borderColor, cardShape)
            .drawWithCache {
                // 顶部高光 - 径向渐变模拟光源照射
                val topGlow = Brush.radialGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = highlightAlpha),
                        highlightColor.copy(alpha = highlightAlpha * 0.25f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.25f),
                    radius = size.width * 0.75f,
                )
                // 底部阴影 - 增加立体感
                val bottomShadow = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.06f),
                    ),
                    startY = size.height * 0.65f,
                    endY = size.height,
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(topGlow)
                    drawRect(bottomShadow)
                }
            },
        contentAlignment = contentAlignment,
    ) {
        // 内层边框增加层次感
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(1.dp)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(cornerRadius - 1.dp)
                )
                .padding(horizontal = 19.dp, vertical = 17.dp),
            content = content,
        )
    }
}

/**
 * 预设的净资产卡片（金色渐变）
 */
@Composable
fun NetAssetGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    GradientGlassCard(
        modifier = modifier,
        gradientColors = listOf(
            Color(0xFFF7D986),
            Color(0xFFF2CC68),
            Color(0xFFE7B953),
        ),
        highlightAlpha = 0.50f,
        content = content,
    )
}

/**
 * 预设的月度支出卡片（蓝色渐变）
 */
@Composable
fun MonthlyExpenseGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    GradientGlassCard(
        modifier = modifier,
        gradientColors = listOf(
            Color(0xFF2E56D8),
            Color(0xFF3F6EEA),
            Color(0xFF5C8CF8),
        ),
        highlightAlpha = 0.40f,
        content = content,
    )
}

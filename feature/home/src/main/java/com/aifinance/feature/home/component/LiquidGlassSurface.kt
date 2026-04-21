package com.aifinance.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    baseColor: Color = Color(0xFFF6F7FB).copy(alpha = 0.88f),
    borderColor: Color = Color.White.copy(alpha = 0.66f),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val glassShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(glassShape)
            .background(baseColor)
            .border(width = 1.dp, color = borderColor, shape = glassShape)
            .drawWithCache {
                val topGlow = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.22f,
                )
                val depthTint = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFAAB8D6).copy(alpha = 0.05f),
                    ),
                    startY = size.height * 0.68f,
                    endY = size.height,
                )
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(topGlow)
                    drawRoundRect(depthTint)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

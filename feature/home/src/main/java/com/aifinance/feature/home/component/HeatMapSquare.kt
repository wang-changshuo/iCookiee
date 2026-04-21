package com.aifinance.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DayActivity { None, ExpenseOnly, WithIncome }

@Composable
fun HeatMapSquare(
    activity: DayActivity,
    isToday: Boolean,
    dayNumber: Int = 0,
    daysInMonth: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (activity) {
        DayActivity.WithIncome -> Color(0xFFFF9800)
        DayActivity.ExpenseOnly -> Color(0xFFA1B9F8)
        DayActivity.None -> Color(0xFFF0F1F4)
    }

    val todayPrimaryColor = Color(0xFF2E5FE6)

    val shouldShowDayNumber = !isToday &&
        (dayNumber == 1 || dayNumber == 15 || dayNumber == daysInMonth)

    val dayNumberColor = when (activity) {
        DayActivity.WithIncome -> Color.White.copy(alpha = 0.92f)
        DayActivity.ExpenseOnly -> Color.Black.copy(alpha = 0.60f)
        DayActivity.None -> Color.Black.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isToday) {
            Text(
                text = "今",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (activity == DayActivity.None) todayPrimaryColor else Color.White
            )
        } else if (shouldShowDayNumber) {
            Text(
                text = dayNumber.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = dayNumberColor
            )
        }
    }
}

package com.aifinance.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RecordHeatMap(
    currentMonth: YearMonth,
    monthActivity: Map<Int, DayActivity>,
    onDateClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysInMonth = currentMonth.lengthOfMonth()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 240.dp),
            userScrollEnabled = false
        ) {
            items(daysInMonth) { index ->
                val day = index + 1
                val activity = monthActivity[day] ?: DayActivity.None
                val isToday = day == today.dayOfMonth &&
                        currentMonth.monthValue == today.monthValue &&
                        currentMonth.year == today.year

                HeatMapSquare(
                    activity = activity,
                    isToday = isToday,
                    dayNumber = day,
                    daysInMonth = daysInMonth,
                    onClick = { onDateClick(day) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

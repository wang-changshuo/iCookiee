package com.aifinance.feature.home

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.designsystem.theme.SurfaceSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Composable
fun AppDateTimePickerDialog(
    initialDateTime: LocalDateTime,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var hourText by remember(initialDateTime) {
        mutableStateOf("%02d".format(initialDateTime.hour))
    }
    var minuteText by remember(initialDateTime) {
        mutableStateOf("%02d".format(initialDateTime.minute))
    }
    var displayMonth by remember { mutableStateOf(YearMonth.from(initialDateTime)) }
    var showMonthWheel by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfacePrimary,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceSecondary.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = IcokieTextStyles.titleLarge,
                    color = OnSurfacePrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (showMonthWheel) {
                    MonthWheelPicker(
                        year = displayMonth.year,
                        month = displayMonth.monthValue,
                        onYearMonthChange = { year, month ->
                            displayMonth = YearMonth.of(year, month)
                            selectedDate = selectedDate
                                .withYear(year)
                                .withMonth(month)
                                .withDayOfMonth(
                                    minOf(selectedDate.dayOfMonth, YearMonth.of(year, month).lengthOfMonth())
                                )
                        }
                    )
                } else {
                    MonthCalendar(
                        month = displayMonth,
                        selectedDate = selectedDate,
                        onToggleYearMonthWheel = { showMonthWheel = true },
                        onPreviousMonth = { displayMonth = displayMonth.minusMonths(1) },
                        onNextMonth = { displayMonth = displayMonth.plusMonths(1) },
                        onDateSelected = { selectedDate = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ManualTimeInputRow(
                    hourText = hourText,
                    minuteText = minuteText,
                    onHourChange = { hourText = it.filter { c -> c.isDigit() }.take(2) },
                    onMinuteChange = { minuteText = it.filter { c -> c.isDigit() }.take(2) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 0
                        val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        onConfirm(selectedDate.atTime(h, m))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                ) {
                    Text("确定", color = Color.White, style = IcokieTextStyles.labelMedium)
                }

                if (showMonthWheel) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "返回日历",
                        style = IcokieTextStyles.labelMedium,
                        color = BrandPrimary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { showMonthWheel = false }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualTimeInputRow(
    hourText: String,
    minuteText: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
) {
    val fieldStyle = IcokieTextStyles.titleMedium.copy(
        color = OnSurfacePrimary,
        textAlign = TextAlign.Center,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSecondary)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "时间",
            style = IcokieTextStyles.bodyLarge,
            color = OnSurfaceSecondary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicTextField(
                value = hourText,
                onValueChange = onHourChange,
                modifier = Modifier.width(44.dp),
                textStyle = fieldStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        inner()
                    }
                },
            )
            Text(":", style = IcokieTextStyles.titleMedium, color = OnSurfacePrimary)
            BasicTextField(
                value = minuteText,
                onValueChange = onMinuteChange,
                modifier = Modifier.width(44.dp),
                textStyle = fieldStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    onToggleYearMonthWheel: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onToggleYearMonthWheel)
            ) {
                Text(
                    text = "${month.year}年${month.monthValue}月",
                    style = IcokieTextStyles.titleLarge,
                    color = OnSurfacePrimary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = ">", style = IcokieTextStyles.titleMedium, color = OnSurfaceSecondary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthNavButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onPreviousMonth)
                MonthNavButton(icon = Icons.AutoMirrored.Filled.ArrowForward, onClick = onNextMonth)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekday ->
                Text(
                    text = weekday,
                    style = IcokieTextStyles.labelSmall,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.width(32.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val firstDayOfMonth = month.atDay(1)
        val startOffset = firstDayOfMonth.dayOfWeek.toWeekIndex()
        val daysInMonth = month.lengthOfMonth()
        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

        repeat(totalCells / 7) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(7) { dayIndex ->
                    val cellIndex = weekIndex * 7 + dayIndex
                    val dayNumber = cellIndex - startOffset + 1
                    val isValidDay = dayNumber in 1..daysInMonth

                    if (!isValidDay) {
                        Spacer(modifier = Modifier.size(32.dp))
                    } else {
                        val date = month.atDay(dayNumber)
                        val selected = date == selectedDate

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (selected) BrandPrimary else Color.Transparent)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                color = if (selected) Color.White else OnSurfacePrimary,
                                style = IcokieTextStyles.bodyLarge,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MonthNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(SurfaceSecondary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurfaceSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MonthWheelPicker(
    year: Int,
    month: Int,
    onYearMonthChange: (Int, Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 2000
                    maxValue = 2100
                    value = year
                    wrapSelectorWheel = false
                    setOnValueChangedListener { _, _, newVal ->
                        onYearMonthChange(newVal, month)
                    }
                }
            },
            update = { picker ->
                if (picker.value != year) {
                    picker.value = year
                }
                picker.setOnValueChangedListener { _, _, newVal ->
                    onYearMonthChange(newVal, month)
                }
            }
        )

        Text(text = "年", style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)

        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 1
                    maxValue = 12
                    value = month
                    wrapSelectorWheel = false
                    setOnValueChangedListener { _, _, newVal ->
                        onYearMonthChange(year, newVal)
                    }
                }
            },
            update = { picker ->
                if (picker.value != month) {
                    picker.value = month
                }
                picker.setOnValueChangedListener { _, _, newVal ->
                    onYearMonthChange(year, newVal)
                }
            }
        )

        Text(text = "月", style = IcokieTextStyles.bodyLarge, color = OnSurfaceSecondary)
    }
}

private fun DayOfWeek.toWeekIndex(): Int {
    return when (this) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}

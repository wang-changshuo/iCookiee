package com.aifinance.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HeatMapSquareTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun day1_notToday_activityNone_showsDayNumber() {
        composeTestRule.setContent {
            HeatMapSquareWrapper(
                activity = DayActivity.None,
                isToday = false,
                dayNumber = 1,
                daysInMonth = 31
            )
        }
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun day15_notToday_activityExpenseOnly_showsDayNumber() {
        composeTestRule.setContent {
            HeatMapSquareWrapper(
                activity = DayActivity.ExpenseOnly,
                isToday = false,
                dayNumber = 15,
                daysInMonth = 31
            )
        }
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun day31_notToday_activityWithIncome_showsDayNumber() {
        composeTestRule.setContent {
            HeatMapSquareWrapper(
                activity = DayActivity.WithIncome,
                isToday = false,
                dayNumber = 31,
                daysInMonth = 31
            )
        }
        composeTestRule.onNodeWithText("31").assertIsDisplayed()
    }

    @Test
    fun day15_isToday_showsJinInsteadOfDayNumber() {
        composeTestRule.setContent {
            HeatMapSquareWrapper(
                activity = DayActivity.ExpenseOnly,
                isToday = true,
                dayNumber = 15,
                daysInMonth = 31
            )
        }
        composeTestRule.onNodeWithText("今").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertDoesNotExist()
    }

    @Test
    fun day2_notToday_doesNotShowDayNumber() {
        composeTestRule.setContent {
            HeatMapSquareWrapper(
                activity = DayActivity.None,
                isToday = false,
                dayNumber = 2,
                daysInMonth = 31
            )
        }
        composeTestRule.onNodeWithText("2").assertDoesNotExist()
    }
}

@Composable
private fun HeatMapSquareWrapper(
    activity: DayActivity,
    isToday: Boolean,
    dayNumber: Int,
    daysInMonth: Int = 31,
    onClick: () -> Unit = {}
) {
    HeatMapSquare(
        activity = activity,
        isToday = isToday,
        dayNumber = dayNumber,
        daysInMonth = daysInMonth,
        onClick = onClick
    )
}

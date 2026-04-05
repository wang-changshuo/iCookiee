package com.aifinance.feature.statistics.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.statistics.StatisticsScreen

const val STATISTICS_ROUTE = "statistics"

fun NavController.navigateToStatistics(navOptions: NavOptions? = null) {
    navigate(STATISTICS_ROUTE, navOptions)
}

fun NavGraphBuilder.statisticsScreen(
    onBack: () -> Unit,
) {
    composable(STATISTICS_ROUTE) {
        StatisticsScreen(onBack = onBack)
    }
}

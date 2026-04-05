package com.aifinance.feature.scheduled.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.scheduled.ScheduledTransactionScreen

const val SCHEDULED_TRANSACTION_ROUTE = "scheduled_transaction"

fun NavController.navigateToScheduledTransaction(navOptions: NavOptions? = null) {
    navigate(SCHEDULED_TRANSACTION_ROUTE, navOptions)
}

fun NavGraphBuilder.scheduledTransactionScreen(
    onBack: () -> Unit,
) {
    composable(SCHEDULED_TRANSACTION_ROUTE) {
        ScheduledTransactionScreen(onBack = onBack)
    }
}

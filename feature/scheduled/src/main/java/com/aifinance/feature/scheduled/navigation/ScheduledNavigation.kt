package com.aifinance.feature.scheduled.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.scheduled.ScheduledTransactionAddScreen
import com.aifinance.feature.scheduled.ScheduledTransactionListScreen

const val SCHEDULED_TRANSACTION_ROUTE = "scheduled_transaction"
const val SCHEDULED_ADD_ROUTE = "scheduled_transaction_add"

fun NavController.navigateToScheduledTransaction(navOptions: NavOptions? = null) {
    navigate(SCHEDULED_TRANSACTION_ROUTE, navOptions)
}

fun NavController.navigateToScheduledTransactionAdd() {
    navigate(SCHEDULED_ADD_ROUTE)
}

fun NavGraphBuilder.scheduledTransactionScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
) {
    composable(SCHEDULED_TRANSACTION_ROUTE) {
        ScheduledTransactionListScreen(
            onBack = onBack,
            onNavigateToAdd = onNavigateToAdd,
        )
    }
}

fun NavGraphBuilder.scheduledTransactionAddScreen(
    onBack: () -> Unit,
) {
    composable(SCHEDULED_ADD_ROUTE) {
        ScheduledTransactionAddScreen(onBack = onBack)
    }
}

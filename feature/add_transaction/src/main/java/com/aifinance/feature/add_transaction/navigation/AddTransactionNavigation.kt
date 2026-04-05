package com.aifinance.feature.add_transaction.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.add_transaction.AddTransactionScreen

const val ADD_TRANSACTION_ROUTE = "add_transaction"

fun NavController.navigateToAddTransaction(navOptions: NavOptions? = null) {
    navigate(ADD_TRANSACTION_ROUTE, navOptions)
}

fun NavGraphBuilder.addTransactionScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    composable(ADD_TRANSACTION_ROUTE) {
        AddTransactionScreen(
            onBack = onBack,
            onSuccess = onSuccess
        )
    }
}

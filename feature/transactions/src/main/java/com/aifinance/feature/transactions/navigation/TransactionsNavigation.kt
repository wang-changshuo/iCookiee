package com.aifinance.feature.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aifinance.feature.transactions.TransactionsScreen
import com.aifinance.feature.transactions.TransactionDetailRoute
import java.util.UUID

const val TRANSACTIONS_ROUTE = "transactions"
const val TRANSACTION_DETAIL_ROUTE = "transaction_detail/{transactionId}"

fun transactionDetailRoute(transactionId: UUID): String = "transaction_detail/$transactionId"

fun NavController.navigateToTransactions(navOptions: NavOptions? = null) {
    navigate(TRANSACTIONS_ROUTE, navOptions)
}

fun NavController.navigateToTransactionDetail(transactionId: UUID, navOptions: NavOptions? = null) {
    navigate(transactionDetailRoute(transactionId), navOptions)
}

fun NavGraphBuilder.transactionsScreen(
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
) {
    composable(TRANSACTIONS_ROUTE) {
        TransactionsScreen(
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
        )
    }
}

fun NavGraphBuilder.transactionDetailScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    composable(
        route = TRANSACTION_DETAIL_ROUTE,
        arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val transactionIdArg = backStackEntry.arguments?.getString("transactionId")
        TransactionDetailRoute(
            transactionIdArg = transactionIdArg,
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}

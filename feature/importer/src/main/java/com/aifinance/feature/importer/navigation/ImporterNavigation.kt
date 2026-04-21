package com.aifinance.feature.importer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.importer.ImporterScreen

const val BILL_IMPORT_ROUTE = "bill_import"

fun NavController.navigateToBillImport(navOptions: NavOptions? = null) {
    navigate(BILL_IMPORT_ROUTE, navOptions)
}

fun NavGraphBuilder.billImportScreen(
    onBack: () -> Unit,
) {
    composable(BILL_IMPORT_ROUTE) {
        ImporterScreen(onBack = onBack)
    }
}

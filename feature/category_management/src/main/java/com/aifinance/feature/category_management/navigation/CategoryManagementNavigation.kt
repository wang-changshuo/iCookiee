package com.aifinance.feature.category_management.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.category_management.CategoryManagementScreen

const val CATEGORY_MANAGEMENT_ROUTE = "category_management"

fun NavController.navigateToCategoryManagement(navOptions: NavOptions? = null) {
    navigate(CATEGORY_MANAGEMENT_ROUTE, navOptions)
}

fun NavGraphBuilder.categoryManagementScreen(
    onBack: () -> Unit,
) {
    composable(CATEGORY_MANAGEMENT_ROUTE) {
        CategoryManagementScreen(onBack = onBack)
    }
}

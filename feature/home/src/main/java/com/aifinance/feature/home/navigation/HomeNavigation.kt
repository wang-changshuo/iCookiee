package com.aifinance.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.home.HomeContainerScreen
import java.util.UUID

const val HOME_ROUTE = "home"

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(HOME_ROUTE, navOptions)
}

fun NavGraphBuilder.homeScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToAssetManagement: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
) {
    composable(HOME_ROUTE) {
        HomeContainerScreen(
            onOpenDrawer = onOpenDrawer,
            onNavigateToAssetManagement = onNavigateToAssetManagement,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
        )
    }
}

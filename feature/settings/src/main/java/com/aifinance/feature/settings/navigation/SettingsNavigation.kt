package com.aifinance.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.settings.SettingsScreen

const val SETTINGS_ROUTE = "settings"

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(SETTINGS_ROUTE, navOptions)
}

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(onBack = onBack)
    }
}

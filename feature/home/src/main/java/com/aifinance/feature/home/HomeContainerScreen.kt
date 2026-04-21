package com.aifinance.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun HomeContainerScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToAssetManagement: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTopTab by rememberSaveable { mutableStateOf(HomeTopTab.RECORD) }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            TopRecordAiBar(
                selectedTab = selectedTopTab,
                onMenuClick = onOpenDrawer,
                onTabSelected = { selectedTopTab = it },
                modifier = Modifier.padding(top = 4.dp),
            )

            when (selectedTopTab) {
                HomeTopTab.RECORD -> {
                    RecordHomeContent(
                        onNavigateToAssetManagement = onNavigateToAssetManagement,
                        onNavigateToStatistics = onNavigateToStatistics,
                        onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                    )
                }

                HomeTopTab.AI_ASSISTANT -> {
                    AiAssistantScreen()
                }
            }
        }
    }
}

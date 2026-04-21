package com.aifinance.feature.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.ScheduledRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTransactionListScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: ScheduledTransactionViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SurfacePrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "定时记账",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OnSurfaceSecondary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAdd) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加定时记账",
                            tint = BrandPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePrimary,
                    titleContentColor = OnSurfacePrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(padding),
        ) {
            if (rules.isEmpty()) {
                ScheduledEmptyState(onAddClick = onNavigateToAdd)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        val category = resolveCategory(rule, allCategories)
                        ScheduledRuleListCard(
                            rule = rule,
                            category = category,
                            onToggleEnabled = {
                                viewModel.setRuleEnabled(rule, !rule.enabled)
                            },
                            onDelete = { viewModel.deleteRule(rule.id) },
                        )
                    }
                    item {
                        Text(
                            text = "- 到底了 -",
                            style = IcokieTextStyles.labelSmall,
                            color = OnSurfaceTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun resolveCategory(rule: ScheduledRule, all: List<Category>): Category? {
    val id = rule.categoryId ?: return null
    return all.find { it.id == id }
        ?: CategoryCatalog.findById(id)?.asCategory()
}

@Composable
private fun ScheduledEmptyState(
    onAddClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "工资、住房…每月重复记账很麻烦？",
            style = IcokieTextStyles.bodyLarge,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "固定周期的收支，自动帮你记～",
            style = IcokieTextStyles.bodyMedium,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = OnPrimary,
                )
                Text(
                    text = "添加",
                    color = OnPrimary,
                    style = IcokieTextStyles.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

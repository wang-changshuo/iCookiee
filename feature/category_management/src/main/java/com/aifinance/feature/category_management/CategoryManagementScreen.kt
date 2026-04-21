package com.aifinance.feature.category_management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.model.Category
import com.aifinance.core.model.TransactionType

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CategoryManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var inputName by remember(selectedType) { mutableStateOf("") }

    val categories = when (selectedType) {
        TransactionType.EXPENSE -> uiState.expenseCategories
        TransactionType.INCOME -> uiState.incomeCategories
        TransactionType.TRANSFER -> emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("分类管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color(0xFFF1F5FB),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryTabs(
                selectedType = selectedType,
                onSelectExpense = { selectedType = TransactionType.EXPENSE },
                onSelectIncome = { selectedType = TransactionType.INCOME },
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (selectedType == TransactionType.EXPENSE) "新增支出分类" else "新增收入分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入分类名称，例如：奶茶 / Salary") },
                    )
                    Text(
                        text = "图标会自动使用分类名称的第一个汉字或首字母。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                    )
                    Button(
                        onClick = {
                            viewModel.addCustomCategory(selectedType, inputName)
                            inputName = ""
                        },
                        enabled = inputName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("添加分类")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(items = categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onDelete = { viewModel.deleteCategory(category) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedType: TransactionType,
    onSelectExpense: () -> Unit,
    onSelectIncome: () -> Unit,
) {
    TabRow(selectedTabIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1) {
        Tab(
            selected = selectedType == TransactionType.EXPENSE,
            onClick = onSelectExpense,
            text = { Text("支出") },
        )
        Tab(
            selected = selectedType == TransactionType.INCOME,
            onClick = onSelectIncome,
            text = { Text("收入") },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val bgColor = Color(category.color)
            val textColor = if (bgColor.luminance() > 0.5f) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF111827),
                )
                Text(
                    text = if (category.isDefault) "系统分类" else "自定义分类",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (category.isDefault) Color(0xFF64748B) else Color(0xFF2563EB),
                )
            }
        }

        if (!category.isDefault) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除分类",
                    tint = Color(0xFFDC2626),
                )
            }
        }
    }
}

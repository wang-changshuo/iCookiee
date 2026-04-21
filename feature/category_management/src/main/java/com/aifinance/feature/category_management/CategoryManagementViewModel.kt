package com.aifinance.feature.category_management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoryManagementUiState> =
        combine(
            categoryRepository.getCategoriesByType(TransactionType.EXPENSE),
            categoryRepository.getCategoriesByType(TransactionType.INCOME),
        ) { expenseCustom, incomeCustom ->
            CategoryManagementUiState(
                expenseCategories = mergeDefaultAndCustom(TransactionType.EXPENSE, expenseCustom),
                incomeCategories = mergeDefaultAndCustom(TransactionType.INCOME, incomeCustom),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryManagementUiState(),
        )

    fun addCustomCategory(type: TransactionType, name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val nextOrder = when (type) {
                TransactionType.EXPENSE -> uiState.value.expenseCategories.size
                TransactionType.INCOME -> uiState.value.incomeCategories.size
                TransactionType.TRANSFER -> uiState.value.expenseCategories.size
            }

            categoryRepository.insertCategory(
                Category(
                    name = normalizedName,
                    icon = categoryIconText(normalizedName),
                    color = generatedColor(type, normalizedName),
                    type = type,
                    isDefault = false,
                    order = nextOrder,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        if (category.isDefault) {
            return
        }

        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    private fun mergeDefaultAndCustom(type: TransactionType, custom: List<Category>): List<Category> {
        val defaults = CategoryCatalog.forType(type).map { it.asCategory() }
        val customItems = custom
            .filter { it.type == type && !it.isDefault }
            .sortedBy { it.order }

        return defaults + customItems
    }
}

data class CategoryManagementUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
)

private fun categoryIconText(name: String): String {
    val first = name.firstOrNull() ?: '#'
    return when {
        first.code in 0x4E00..0x9FFF -> first.toString()
        first.isLetter() -> first.uppercaseChar().toString()
        else -> "#"
    }
}

private fun generatedColor(type: TransactionType, name: String): Int {
    val palette = when (type) {
        TransactionType.EXPENSE -> listOf(0xFF2E5FE6, 0xFF0EA5E9, 0xFF2563EB, 0xFF14B8A6, 0xFF7C3AED)
        TransactionType.INCOME -> listOf(0xFFAF6A20, 0xFFB7791F, 0xFF16A34A, 0xFF0D9488, 0xFFD97706)
        TransactionType.TRANSFER -> listOf(0xFF6B7280)
    }
    val index = (name.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index].toInt()
}

package com.aifinance.core.data.repository

import com.aifinance.core.database.dao.CategoryDao
import com.aifinance.core.database.entity.CategoryEntity
import com.aifinance.core.model.Category
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: java.util.UUID): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }
}

private fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = TransactionType.valueOf(type),
        parentId = parentId,
        isDefault = isDefault,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type.name,
        parentId = parentId,
        isDefault = isDefault,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

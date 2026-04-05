package com.aifinance.core.model

import java.util.UUID

data class CatalogCategory(
    val id: UUID,
    val name: String,
    val icon: String,
    val color: Int,
    val type: TransactionType,
    val order: Int,
) {
    fun asCategory(): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            type = type,
            isDefault = true,
            order = order,
        )
    }
}

object CategoryCatalog {
    object Ids {
        val ExpenseFood: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val ExpenseShopping: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val ExpenseTransport: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val IncomeSalary: UUID = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val ExpenseHousing: UUID = UUID.fromString("55555555-5555-5555-5555-555555555555")
        val ExpenseCommunication: UUID = UUID.fromString("66666666-6666-6666-6666-666666666666")
        val ExpenseOther: UUID = UUID.fromString("77777777-7777-7777-7777-777777777777")
        val TransferDefault: UUID = UUID.fromString("88888888-8888-8888-8888-888888888888")

        val IncomeBonus: UUID = UUID.fromString("99999999-9999-9999-9999-999999999991")
        val IncomePartTime: UUID = UUID.fromString("99999999-9999-9999-9999-999999999992")
        val IncomeInvestment: UUID = UUID.fromString("99999999-9999-9999-9999-999999999993")
        val IncomeReimbursement: UUID = UUID.fromString("99999999-9999-9999-9999-999999999994")
        val IncomeGift: UUID = UUID.fromString("99999999-9999-9999-9999-999999999995")

        val IncomeCareer: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1")
        val IncomeBusiness: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2")
        val IncomeInsurance: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3")
        val IncomeTransfer: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4")
        val IncomeSecondHand: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5")
        val IncomeLucky: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6")
        val IncomeLiving: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7")
        val IncomeOther: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8")

        val ExpenseMedical: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1")
        val ExpenseEducation: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2")
        val ExpenseEntertainment: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3")
    }

    private val expenseCategories: List<CatalogCategory> = listOf(
        CatalogCategory(Ids.ExpenseFood, "餐饮", "🍜", 0xFF3267CC.toInt(), TransactionType.EXPENSE, 0),
        CatalogCategory(Ids.ExpenseShopping, "购物", "🛍️", 0xFF7B50BE.toInt(), TransactionType.EXPENSE, 1),
        CatalogCategory(Ids.ExpenseTransport, "交通", "🚗", 0xFF2D8B62.toInt(), TransactionType.EXPENSE, 2),
        CatalogCategory(Ids.ExpenseHousing, "住房", "🏠", 0xFF8B5CF6.toInt(), TransactionType.EXPENSE, 3),
        CatalogCategory(Ids.ExpenseCommunication, "通讯", "📱", 0xFF14B8A6.toInt(), TransactionType.EXPENSE, 4),
        CatalogCategory(Ids.ExpenseMedical, "医疗", "💊", 0xFFEF4444.toInt(), TransactionType.EXPENSE, 5),
        CatalogCategory(Ids.ExpenseEducation, "教育", "📚", 0xFF0EA5E9.toInt(), TransactionType.EXPENSE, 6),
        CatalogCategory(Ids.ExpenseEntertainment, "娱乐", "🎮", 0xFFF97316.toInt(), TransactionType.EXPENSE, 7),
        CatalogCategory(Ids.ExpenseOther, "其他支出", "📦", 0xFF6B7280.toInt(), TransactionType.EXPENSE, 8),
    )

    private val incomeCategories: List<CatalogCategory> = listOf(
        CatalogCategory(Ids.IncomeCareer, "职业收入", "💼", 0xFFAF6A20.toInt(), TransactionType.INCOME, 0),
        CatalogCategory(Ids.IncomeBusiness, "经营收入", "🏪", 0xFFB7791F.toInt(), TransactionType.INCOME, 1),
        CatalogCategory(Ids.IncomeInsurance, "保险理财", "🛡️", 0xFF0891B2.toInt(), TransactionType.INCOME, 2),
        CatalogCategory(Ids.IncomeTransfer, "资金往来", "💱", 0xFF0D9488.toInt(), TransactionType.INCOME, 3),
        CatalogCategory(Ids.IncomeSecondHand, "二手买卖", "♻️", 0xFF2563EB.toInt(), TransactionType.INCOME, 4),
        CatalogCategory(Ids.IncomeLucky, "好运收入", "🍀", 0xFFD97706.toInt(), TransactionType.INCOME, 5),
        CatalogCategory(Ids.IncomeLiving, "生活费", "💰", 0xFF7C3AED.toInt(), TransactionType.INCOME, 6),
        CatalogCategory(Ids.IncomeOther, "其他收入", "📥", 0xFF6B7280.toInt(), TransactionType.INCOME, 7),
    )

    private val transferCategories: List<CatalogCategory> = listOf(
        CatalogCategory(Ids.TransferDefault, "转账", "💸", 0xFF5E6A7C.toInt(), TransactionType.TRANSFER, 0),
    )

    private val allCategories: List<CatalogCategory> = expenseCategories + incomeCategories + transferCategories

    val all: List<CatalogCategory>
        get() = allCategories

    fun forType(type: TransactionType): List<CatalogCategory> {
        return when (type) {
            TransactionType.EXPENSE -> expenseCategories
            TransactionType.INCOME -> incomeCategories
            TransactionType.TRANSFER -> transferCategories
        }
    }

    fun categoriesForType(type: TransactionType): List<Category> {
        return forType(type).map { it.asCategory() }
    }

    fun allCategories(): List<Category> {
        return all.map { it.asCategory() }
    }

    fun findById(categoryId: UUID?): CatalogCategory? {
        if (categoryId == null) return null
        return allCategories.firstOrNull { it.id == categoryId }
    }

    fun fallback(type: TransactionType): CatalogCategory {
        return forType(type).first()
    }

    fun resolve(categoryId: UUID?, type: TransactionType): CatalogCategory {
        val matched = findById(categoryId)
        return if (matched != null && matched.type == type) {
            matched
        } else {
            fallback(type)
        }
    }
}

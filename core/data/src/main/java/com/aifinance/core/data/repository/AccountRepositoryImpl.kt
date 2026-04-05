package com.aifinance.core.data.repository

import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.entity.AccountEntity
import com.aifinance.core.model.Account
import com.aifinance.core.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getActiveAccounts(): Flow<List<Account>> {
        return accountDao.getActiveAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: UUID): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun getDefaultIncomeExpenseAccount(): Account? {
        return accountDao.getDefaultIncomeExpenseAccount()?.toDomain()
    }

    override suspend fun getFirstActiveAccount(): Account? {
        return accountDao.getFirstActiveAccount()?.toDomain()
    }

    override suspend fun insertAccount(account: Account) {
        if (account.isDefaultIncomeExpense) {
            accountDao.clearDefaultIncomeExpenseAccount()
        }
        // Ensure currentBalance is synced with initialBalance for new accounts
        val accountWithSyncedBalance = account.copy(
            currentBalance = account.initialBalance
        )
        accountDao.insert(accountWithSyncedBalance.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        if (account.isDefaultIncomeExpense) {
            accountDao.clearDefaultIncomeExpenseAccount()
        }
        accountDao.update(account.copy(updatedAt = Instant.now()).toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.delete(account.toEntity())
    }
}

private fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        currency = currency,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        color = color,
        icon = icon,
        note = note,
        isArchived = isArchived,
        includeInTotalAssets = includeInTotalAssets,
        isDefaultIncomeExpense = isDefaultIncomeExpense,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        type = type.name,
        currency = currency,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        color = color,
        icon = icon,
        note = note,
        isArchived = isArchived,
        includeInTotalAssets = includeInTotalAssets,
        isDefaultIncomeExpense = isDefaultIncomeExpense,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

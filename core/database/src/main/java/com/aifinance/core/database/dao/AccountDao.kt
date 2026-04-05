package com.aifinance.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aifinance.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: UUID): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isArchived = 0 AND isDefaultIncomeExpense = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getDefaultIncomeExpenseAccount(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstActiveAccount(): AccountEntity?

    @Query("UPDATE accounts SET isDefaultIncomeExpense = 0 WHERE isDefaultIncomeExpense = 1")
    suspend fun clearDefaultIncomeExpenseAccount()

    @Query("UPDATE accounts SET currentBalance = currentBalance + :delta WHERE id = :accountId")
    suspend fun adjustCurrentBalance(accountId: UUID, delta: BigDecimal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}

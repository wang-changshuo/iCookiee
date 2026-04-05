package com.aifinance.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aifinance.core.database.entity.ScheduledRuleEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ScheduledRuleDao {
    @Query("SELECT * FROM scheduled_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScheduledRuleEntity>>

    @Query("SELECT * FROM scheduled_rules WHERE enabled = 1")
    suspend fun getAllEnabled(): List<ScheduledRuleEntity>

    @Query("SELECT * FROM scheduled_rules WHERE id = :id")
    suspend fun getById(id: UUID): ScheduledRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledRuleEntity)

    @Update
    suspend fun update(entity: ScheduledRuleEntity)

    @Delete
    suspend fun delete(entity: ScheduledRuleEntity)
}

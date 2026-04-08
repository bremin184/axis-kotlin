package com.axis.app.data.database

import androidx.room.*
import com.axis.app.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: SavingsGoal): Long

    @Query("SELECT * FROM savings_goals ORDER BY isPrimary DESC, deadline ASC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals ORDER BY isPrimary DESC, deadline ASC")
    suspend fun getAllGoalsList(): List<SavingsGoal>

    @Query("UPDATE savings_goals SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE savings_goals SET isPrimary = 1 WHERE id = :goalId")
    suspend fun setPrimary(goalId: Long)

    @Query("UPDATE savings_goals SET currentAmount = :amount WHERE id = :goalId")
    suspend fun updateAmount(goalId: Long, amount: Double)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

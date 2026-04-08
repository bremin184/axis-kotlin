package com.axis.app.data.database

import androidx.room.*
import com.axis.app.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<Budget>)

    @Query("SELECT * FROM budgets WHERE isActive = 1")
    fun getActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE isActive = 1")
    suspend fun getActiveBudgetsList(): List<Budget>

    @Query("SELECT * FROM budgets WHERE category = :category")
    suspend fun getByCategory(category: String): Budget?

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

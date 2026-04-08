package com.axis.app.data.repository

import com.axis.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface PesaRepository {
    val allTransactions: Flow<List<Transaction>>
    val allGoals: Flow<List<SavingsGoal>>
    val allCategories: Flow<List<Category>>
    val allSavingsAccounts: Flow<List<SavingsAccount>>
    val allAccountEntities: Flow<List<AccountEntity>>

    suspend fun getAllTransactionsList(): List<Transaction>
    suspend fun getAllCategoriesList(): List<Category>
    suspend fun getLatestBalance(): Double
    suspend fun getBusinessBalance(): Double
    suspend fun getMonthlyIncome(start: Long? = null, end: Long? = null): Double
    suspend fun getMonthlyExpenses(start: Long? = null, end: Long? = null): Double
    suspend fun getActiveBudgetsList(): List<Budget>
    suspend fun getMonthlyCategoryTotals(start: Long? = null, end: Long? = null): List<CategoryTotal>
    suspend fun getMonthlyDebtPayments(start: Long? = null, end: Long? = null): Double
    suspend fun getActiveKeywords(): Map<String, String>
    suspend fun insertTransactions(transactions: List<Transaction>): Pair<Int, Int>
    suspend fun upsertBudget(budget: Budget)
    suspend fun initDefaultBudgets()
    suspend fun getAllGoalsList(): List<SavingsGoal>
    suspend fun upsertGoal(goal: SavingsGoal)
    suspend fun updateGoalAmount(goalId: Long, newAmount: Double)
    suspend fun clearAllData()
    suspend fun clearImportedData()
    suspend fun createDefaultCategories()
    suspend fun categorizeTransaction(transactionId: Long, categoryName: String, recipient: String?, rawMessage: String? = null)
    suspend fun learnCategory(keyword: String, category: String)
    suspend fun addCategory(category: Category)
    suspend fun updateCategoryName(categoryId: Long, newName: String)
    suspend fun deleteCategory(categoryId: Long)
    suspend fun getCategoryTotalsForIds(ids: List<Long>): List<CategoryTotal>
    suspend fun upsertAccount(account: AccountEntity)
    suspend fun getAllAccountEntitiesList(): List<AccountEntity>
}

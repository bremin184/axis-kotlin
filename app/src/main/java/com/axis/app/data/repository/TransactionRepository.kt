package com.axis.app.data.repository

import androidx.room.*
import com.axis.app.data.database.*
import com.axis.app.data.model.*
import com.axis.app.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import androidx.room.withTransaction

/**
 * Single source of truth for all data operations.
 * Wraps Room DAOs and provides clean coroutine/Flow APIs.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val activatedServiceDao: ActivatedServiceDao,
    private val savingsAccountDao: SavingsAccountDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val categorizationRuleDao: CategorizationRuleDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val db: AppDatabase
) : PesaRepository {

    private val autoCategorizationService = com.axis.app.domain.service.AutoCategorizationService(categoryRuleDao)

    private val ioDispatcher = kotlinx.coroutines.Dispatchers.IO

    // ========================
    // Transactions
    // ========================

    override val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    override val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertTransaction(transaction: Transaction): Long = kotlinx.coroutines.withContext(ioDispatcher) {
        // Duplicate check by transaction code
        if (transactionDao.countByCode(transaction.transactionCode) > 0) return@withContext -1
        transactionDao.insert(transaction)
    }

    override suspend fun insertTransactions(transactions: List<Transaction>): Pair<Int, Int> = kotlinx.coroutines.withContext(ioDispatcher) {
        var added = 0
        var duplicates = 0
        transactions.forEach { tx ->
            if (transactionDao.countByCode(tx.transactionCode) > 0) {
                duplicates++
            } else {
                // Step 8 & 11: Apply auto-categorization during import
                val autoCategory = autoCategorizationService.categorize(tx)
                val categorized = tx.copy(category = autoCategory)
                
                transactionDao.insert(categorized)
                added++

                // Account Tracking Detection & Linking
                syncAccountTracking(categorized)
            }
        }
        Pair(added, duplicates)
    }

    private suspend fun syncAccountTracking(tx: Transaction) = kotlinx.coroutines.withContext(ioDispatcher) {
        val lower = tx.rawMessage.lowercase()
        val accountInfo = when {
            lower.contains("m-shwari") -> Pair("M-Shwari", AccountType.SAVINGS)
            lower.contains("zidii") -> Pair("Zidii", AccountType.SAVINGS)
            lower.contains("till") -> Pair("Buy Goods", AccountType.BUSINESS)
            lower.contains("poch") -> Pair("Pochi", AccountType.BUSINESS)
            else -> Pair("M-Pesa", AccountType.WALLET) // Default for standard MPESA
        }

        val (name, type) = accountInfo
        var account = accountDao.findByName(name)

        if (account == null) {
            // Auto-create account if it doesn't exist
            val newAccountId = accountDao.upsert(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = tx.balanceAfter,
                    isEnabled = true,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
            // Relink this transaction to the new account
            transactionDao.updateAccountId(tx.id, newAccountId)
        } else {
            // Link transaction to existing account
            transactionDao.updateAccountId(tx.id, account.id)

            // Only update balance if tracking is enabled
            if (account.isEnabled) {
                // Determine latest balance by transactionDateTime (Root requirement)
                val latestTx = transactionDao.getLatestTransactionForAccount(account.id)
                if (latestTx != null) {
                    accountDao.upsert(
                        account.copy(
                            balance = latestTx.balanceAfter,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    override suspend fun getAllTransactionsList() = kotlinx.coroutines.withContext(ioDispatcher) { transactionDao.getAllTransactionsList() }

    fun getByFundType(fundType: FundType) = transactionDao.getByFundType(fundType)

    fun getByCategory(category: String) = transactionDao.getByCategory(category)

    fun getByDateRange(startMillis: Long, endMillis: Long) =
        transactionDao.getByDateRange(startMillis, endMillis)

    fun search(query: String) = transactionDao.search(query)

    override suspend fun getLatestBalance() = kotlinx.coroutines.withContext(ioDispatcher) { 
        transactionDao.getLatestBalance() ?: 0.0 
    }

    override suspend fun getBusinessBalance(): Double = kotlinx.coroutines.withContext(ioDispatcher) {
        transactionDao.getBusinessBalance() ?: 0.0
    }

    suspend fun getTransactionCount() = kotlinx.coroutines.withContext(ioDispatcher) { transactionDao.getCount() }

    // ========================
    // Monthly Aggregates
    // ========================

    private fun getMonthRange(year: Int? = null, month: Int? = null): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        if (year != null) cal.set(Calendar.YEAR, year)
        if (month != null) cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    override suspend fun getMonthlyIncome(start: Long?, end: Long?): Double = kotlinx.coroutines.withContext(ioDispatcher) {
        val (s, e) = if (start != null && end != null) Pair(start, end) else getMonthRange()
        transactionDao.getMonthlyIncome(s, e) ?: 0.0
    }

    override suspend fun getMonthlyExpenses(start: Long?, end: Long?): Double = kotlinx.coroutines.withContext(ioDispatcher) {
        val (s, e) = if (start != null && end != null) Pair(start, end) else getMonthRange()
        transactionDao.getMonthlyExpenses(s, e) ?: 0.0
    }

    override suspend fun getMonthlyCategoryTotals(start: Long?, end: Long?): List<CategoryTotal> = kotlinx.coroutines.withContext(ioDispatcher) {
        val (s, e) = if (start != null && end != null) Pair(start, end) else getMonthRange()
        transactionDao.getMonthlyCategoryTotals(s, e)
    }

    suspend fun getMerchantFrequency(year: Int? = null, month: Int? = null): List<MerchantFrequency> = kotlinx.coroutines.withContext(ioDispatcher) {
        val (start, end) = getMonthRange(year, month)
        transactionDao.getMerchantFrequency(start, end)
    }

    override suspend fun getMonthlyDebtPayments(start: Long?, end: Long?): Double = kotlinx.coroutines.withContext(ioDispatcher) {
        val (s, e) = if (start != null && end != null) Pair(start, end) else getMonthRange()
        transactionDao.getMonthlyDebtPayments(s, e) ?: 0.0
    }

    suspend fun getTransactionsForMonth(year: Int? = null, month: Int? = null): List<Transaction> = kotlinx.coroutines.withContext(ioDispatcher) {
        val (start, end) = getMonthRange(year, month)
        transactionDao.getByDateRangeList(start, end)
    }

    // ========================
    // Budgets
    // ========================

    val activeBudgets: Flow<List<Budget>> = budgetDao.getActiveBudgets()

    override suspend fun getActiveBudgetsList() = kotlinx.coroutines.withContext(ioDispatcher) { budgetDao.getActiveBudgetsList() }

    override suspend fun upsertBudget(budget: Budget) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            budgetDao.upsert(budget)
        }
    }

    override suspend fun initDefaultBudgets() = kotlinx.coroutines.withContext(ioDispatcher) {
        val existing = budgetDao.getActiveBudgetsList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Budget("food_drink", 5000.0),
                Budget("transport", 4000.0),
                Budget("groceries", 8000.0),
                Budget("utilities", 5000.0),
                Budget("shopping", 3000.0),
                Budget("entertainment", 2000.0),
                Budget("airtime", 1000.0),
                Budget("personal", 3000.0),
            )
            budgetDao.upsertAll(defaults)
        }
    }

    // ========================
    // Savings Goals
    // ========================

    override val allGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()

    override val allSavingsAccounts: Flow<List<SavingsAccount>> = savingsAccountDao.getAllSavingsAccounts()

    override suspend fun getAllGoalsList() = kotlinx.coroutines.withContext(ioDispatcher) { savingsGoalDao.getAllGoalsList() }

    override suspend fun upsertGoal(goal: SavingsGoal) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            savingsGoalDao.upsert(goal)
        }
    }

    override suspend fun updateGoalAmount(goalId: Long, newAmount: Double) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            savingsGoalDao.updateAmount(goalId, newAmount)
        }
    }

    suspend fun setPrimaryGoal(goalId: Long) = kotlinx.coroutines.withContext(ioDispatcher) {
        savingsGoalDao.clearPrimary()
        savingsGoalDao.setPrimary(goalId)
    }

    suspend fun deleteGoal(goal: SavingsGoal) = kotlinx.coroutines.withContext(ioDispatcher) { savingsGoalDao.delete(goal) }

    // ========================
    // Activated Services
    // ========================

    val activeServices: Flow<List<ActivatedService>> = activatedServiceDao.getActiveServices()

    override suspend fun getActiveKeywords(): Map<String, String> = kotlinx.coroutines.withContext(ioDispatcher) {
        activatedServiceDao.getActiveServicesList().associate { it.keyword to it.name }
    }

    suspend fun activateService(service: ActivatedService) = kotlinx.coroutines.withContext(ioDispatcher) { activatedServiceDao.upsert(service) }

    suspend fun deactivateService(service: ActivatedService) = kotlinx.coroutines.withContext(ioDispatcher) { activatedServiceDao.delete(service) }

    // ========================
    // Categories
    // ========================
 
    override suspend fun getAllCategoriesList() = kotlinx.coroutines.withContext(ioDispatcher) { categoryDao.getAllCategoriesList() }

    override suspend fun createDefaultCategories() = kotlinx.coroutines.withContext(ioDispatcher) {
        val defaultCategories = listOf(
            Category(name = "Food & Drink", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Transport", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Groceries", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Utilities", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Shopping", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Entertainment", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Airtime", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Personal", isDefault = true, type = CategoryType.EXPENSE_ONLY),
            Category(name = "Salary", isDefault = true, type = CategoryType.INCOME_ONLY),
            Category(name = "Loan", isDefault = true, type = CategoryType.INCOME_AND_EXPENSE)
        )
        defaultCategories.forEach { categoryDao.insert(it) }
        preloadDefaultRules()
    }

    private suspend fun preloadDefaultRules() {
        val defaults = listOf(
            CategoryRuleEntity(keyword = "SAFARICOM", category = "Data & Internet"),
            CategoryRuleEntity(keyword = "KPLC", category = "Utilities"),
            CategoryRuleEntity(keyword = "NAIVAS", category = "Groceries"),
            CategoryRuleEntity(keyword = "QUICKMART", category = "Groceries"),
            CategoryRuleEntity(keyword = "UBER", category = "Transport"),
            CategoryRuleEntity(keyword = "BOLT", category = "Transport")
        )
        // Only insert if no rules exist to avoid duplicates
        if (categoryRuleDao.getAllRules().isEmpty()) {
            defaults.forEach { categoryRuleDao.insertRule(it) }
        }
    }

    // ========================
    // Category Management
    // ========================

    override suspend fun categorizeTransaction(
        transactionId: Long,
        categoryName: String,
        recipient: String?,
        rawMessage: String?
    ) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            // Update this specific transaction
            transactionDao.updateCategory(transactionId, categoryName)

            // Step 6: Learn from user manual categorization
            if (rawMessage != null) {
                val keyword = com.axis.app.domain.util.extractKeyword(rawMessage)
                learnCategory(keyword, categoryName)
            } else if (!recipient.isNullOrBlank()) {
                // Fallback for backward compatibility or when raw message is missing
                learnCategory(recipient, categoryName)
            }
            
            // Relabel past transactions with same recipient if applicable
            if (!recipient.isNullOrBlank()) {
                transactionDao.updateCategoryByRecipient(recipient, categoryName)
            }
        }
    }

    override suspend fun learnCategory(keyword: String, category: String) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            categoryRuleDao.insertRule(
                com.axis.app.data.model.CategoryRuleEntity(
                    keyword = keyword,
                    category = category
                )
            )
        }
    }

    private suspend fun updateAccountBalance(accountId: Long) = kotlinx.coroutines.withContext(ioDispatcher) {
        val latestTx = transactionDao.getLatestTransactionForAccount(accountId)
        val account = accountDao.getAllAccountsList().find { it.id == accountId }
        
        if (latestTx != null && account != null) {
            accountDao.upsert(
                account.copy(
                    balance = latestTx.balanceAfter,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun addCategory(category: Category) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            categoryDao.insert(category)
        }
    }

    override suspend fun updateCategoryName(categoryId: Long, newName: String) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            categoryDao.updateName(categoryId, newName)
        }
    }

    override suspend fun deleteCategory(categoryId: Long) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            val category = categoryDao.getAllCategoriesList().find { it.id == categoryId }
            if (category != null) {
                // Step 10: Delete associated rules when category is deleted
                categoryRuleDao.deleteRulesForCategory(category.name)
            }
            categoryDao.deleteCustomCategory(categoryId)
        }
    }

    override suspend fun getCategoryTotalsForIds(ids: List<Long>): List<CategoryTotal> = kotlinx.coroutines.withContext(ioDispatcher) {
        transactionDao.getCategoryTotalsForIds(ids)
    }

    // ========================
    // Clear All
    // ========================

    override val allAccountEntities: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override suspend fun upsertAccount(account: AccountEntity) {
        kotlinx.coroutines.withContext(ioDispatcher) {
            accountDao.upsert(account)
        }
    }

    override suspend fun getAllAccountEntitiesList(): List<AccountEntity> = kotlinx.coroutines.withContext(ioDispatcher) { accountDao.getAllAccountsList() }

    override suspend fun clearAllData() {
        db.withTransaction {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            savingsGoalDao.deleteAll()
            activatedServiceDao.deleteAll()
            categoryDao.deleteAll()
            // Step 9: Do NOT delete category_rules (Persistent Learning)
            // categorizationRuleDao.deleteAll() // Old dao, we can remove it eventually
            accountDao.deleteAll()
        }
    }

    override suspend fun clearImportedData() {
        db.withTransaction {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            savingsGoalDao.deleteAll()
            // Step 9: Do NOT delete category_rules
            // categorizationRuleDao.deleteAll() 
            savingsAccountDao.deleteAll()
            // Reset balance in AccountEntity but keep the entries (M-Pesa, M-Shwari, etc.)
            accountDao.getAllAccountsList().forEach { acc ->
                accountDao.upsert(acc.copy(balance = 0.0, lastSyncedAt = 0L))
            }
        }
    }
}

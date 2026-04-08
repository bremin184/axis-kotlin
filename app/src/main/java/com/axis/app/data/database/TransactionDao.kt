package com.axis.app.data.database

import androidx.room.*
import com.axis.app.data.model.CategoryTotal
import com.axis.app.data.model.FundType
import com.axis.app.data.model.MerchantFrequency
import com.axis.app.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // --- Inserts ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    // --- Queries: All ---

    @Query("SELECT * FROM transactions ORDER BY transactionDateTime DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY transactionDateTime DESC")
    suspend fun getAllTransactionsList(): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transactionCode = :code)")
    suspend fun existsByCode(code: String): Boolean

    @Query("SELECT COUNT(*) FROM transactions WHERE transactionCode = :code")
    suspend fun countByCode(code: String): Int

    // --- Queries: By Fund Type ---

    @Query("SELECT * FROM transactions WHERE fundType = :fundType ORDER BY transactionDateTime DESC")
    fun getByFundType(fundType: FundType): Flow<List<Transaction>>

    // --- Queries: By Category ---

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY transactionDateTime DESC")
    fun getByCategory(category: String): Flow<List<Transaction>>

    // --- Queries: By Time Range ---

    @Query("SELECT * FROM transactions WHERE transactionDateTime BETWEEN :startMillis AND :endMillis ORDER BY transactionDateTime DESC")
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE transactionDateTime BETWEEN :startMillis AND :endMillis ORDER BY transactionDateTime DESC")
    suspend fun getByDateRangeList(startMillis: Long, endMillis: Long): List<Transaction>

    // --- Queries: Monthly Aggregates ---

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE isIncome = 0 AND transactionDateTime BETWEEN :startMillis AND :endMillis 
        GROUP BY category
    """)
    suspend fun getMonthlyCategoryTotals(startMillis: Long, endMillis: Long): List<CategoryTotal>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND transactionDateTime BETWEEN :startMillis AND :endMillis")
    suspend fun getMonthlyIncome(startMillis: Long, endMillis: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND transactionDateTime BETWEEN :startMillis AND :endMillis")
    suspend fun getMonthlyExpenses(startMillis: Long, endMillis: Long): Double?

    // --- Queries: Merchant Grouping (micro-payment detection) ---

    @Query("""
        SELECT recipient, COUNT(*) as frequency, SUM(amount) as totalAmount
        FROM transactions
        WHERE isIncome = 0 AND transactionDateTime BETWEEN :startMillis AND :endMillis AND recipient IS NOT NULL
        GROUP BY recipient
        ORDER BY frequency DESC
    """)
    suspend fun getMerchantFrequency(startMillis: Long, endMillis: Long): List<MerchantFrequency>

    // --- Queries: Balance ---

    @Query("SELECT balanceAfter FROM transactions ORDER BY transactionDateTime DESC LIMIT 1")
    suspend fun getLatestBalance(): Double?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY transactionDateTime DESC LIMIT 1")
    suspend fun getLatestTransactionForAccount(accountId: Long): Transaction?

    @Query("""
        SELECT SUM(balanceAfter) FROM (
            SELECT balanceAfter FROM transactions 
            WHERE subType IN ('POCHI_BUSINESS', 'BUY_GOODS') 
            GROUP BY subType 
            HAVING transactionDateTime = MAX(transactionDateTime)
        )
    """)
    suspend fun getBusinessBalance(): Double?

    // --- Queries: By Income/Expense ---

    @Query("SELECT * FROM transactions WHERE isIncome = :isIncome ORDER BY transactionDateTime DESC")
    fun getByDirection(isIncome: Boolean): Flow<List<Transaction>>

    // --- Queries: Search ---

    @Query("""
        SELECT * FROM transactions 
        WHERE recipient LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR type LIKE '%' || :query || '%'
           OR subType LIKE '%' || :query || '%'
        ORDER BY transactionDateTime DESC
    """)
    fun search(query: String): Flow<List<Transaction>>

    // --- Queries: Debt ---

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE fundType IN ('SHORT_TERM_LIABILITY', 'LONG_TERM_LIABILITY') 
        AND isIncome = 0
        AND transactionDateTime BETWEEN :startMillis AND :endMillis
    """)
    suspend fun getMonthlyDebtPayments(startMillis: Long, endMillis: Long): Double?

    // --- Update: Category ---

    @Query("UPDATE transactions SET category = :category WHERE id = :transactionId")
    suspend fun updateCategory(transactionId: Long, category: String)

    @Query("UPDATE transactions SET category = :category WHERE recipient = :recipient")
    suspend fun updateCategoryByRecipient(recipient: String, category: String)

    @Query("UPDATE transactions SET accountId = :accountId WHERE id = :transactionId")
    suspend fun updateAccountId(transactionId: Long, accountId: Long)

    // --- Queries: Filtered Totals ---

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions WHERE id IN (:ids) AND isIncome = 0 
        GROUP BY category
    """)
    suspend fun getCategoryTotalsForIds(ids: List<Long>): List<CategoryTotal>

    @Query("SELECT COUNT(*) FROM transactions WHERE fundType IN (:fundTypes) AND transactionDateTime BETWEEN :startMillis AND :endMillis")
    suspend fun getCountByFundTypeInDateRange(fundTypes: List<String>, startMillis: Long, endMillis: Long): Int

    // --- Delete ---

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

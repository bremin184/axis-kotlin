package com.axis.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axis.app.data.model.SavingsAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(savingsAccount: SavingsAccount)

    @Query("SELECT * FROM savings_accounts")
    fun getAllSavingsAccounts(): Flow<List<SavingsAccount>>

    @Query("SELECT * FROM savings_accounts WHERE id = :id")
    suspend fun getSavingsAccount(id: Long): SavingsAccount?

    @Query("SELECT * FROM savings_accounts WHERE type = :type LIMIT 1")
    suspend fun findByType(type: String): SavingsAccount?

    @Query("SELECT * FROM savings_accounts")
    suspend fun getAllSavingsAccountsList(): List<SavingsAccount>

    @Query("DELETE FROM savings_accounts WHERE id = :id")
    suspend fun deleteSavingsAccount(id: Long)

    @Query("DELETE FROM savings_accounts")
    suspend fun deleteAll()
}

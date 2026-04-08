package com.axis.app.data.database

import androidx.room.*
import com.axis.app.data.model.AccountEntity
import com.axis.app.data.model.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isEnabled = 1")
    fun getEnabledAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE type = :type")
    suspend fun getAccountsByType(type: AccountType): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): AccountEntity?

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>): List<Long>

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

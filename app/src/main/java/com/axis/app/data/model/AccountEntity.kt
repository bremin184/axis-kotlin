package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    SAVINGS,
    BUSINESS,
    WALLET,
    CUSTOM
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val isEnabled: Boolean = true,
    val balance: Double = 0.0,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

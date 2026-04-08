package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_accounts")
data class SavingsAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // e.g., M-Shwari, Zidii
    val balance: Double = 0.0
) {
    companion object {
        const val TYPE_MSHWARI = "M-Shwari"
        const val TYPE_ZIDII = "Zidii"
    }
}

package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0, // Used as a fallback or for manual tracking
    val deadline: Long,
    val isPrimary: Boolean = false,
    val savingsAccountId: Long? = null,
    val allocationPercentage: Double? = null // 0.0 to 1.0
)

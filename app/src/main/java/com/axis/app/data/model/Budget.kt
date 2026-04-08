package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Monthly budget limit per spending category.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val category: String,

    /** Monthly spending limit in KES */
    val limitAmount: Double,

    /** Whether this budget is active */
    val isActive: Boolean = true
)

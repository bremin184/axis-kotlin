package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val type: CategoryType = CategoryType.EXPENSE_ONLY,
    val createdAt: Long = System.currentTimeMillis()
)

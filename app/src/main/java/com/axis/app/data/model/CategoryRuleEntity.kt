package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,  // example: "SAFARICOM"
    val category: String, // example: "Data & Internet"
    val isIncomeRule: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

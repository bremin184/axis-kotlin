package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorization_rules")
data class CategorizationRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientIdentifier: String,
    val categoryId: Long,
    val categoryName: String = ""
)

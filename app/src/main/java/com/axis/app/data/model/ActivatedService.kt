package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activated_services")
data class ActivatedService(
    @PrimaryKey
    val keyword: String,
    val name: String,
    val description: String,
    val isActive: Boolean = true
)

package com.axis.app.domain.budget

import com.axis.app.data.model.Category

enum class BudgetAlertLevel {
    ON_TRACK,
    WATCH,
    WARNING,
    EXCEEDED
}

data class CategoryBudgetStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentage: Float,
    val alertLevel: BudgetAlertLevel
)

package com.axis.app.domain.budget

data class BudgetOverview(
    val totalBudget: Double,
    val totalSpent: Double,
    val overallPercentage: Float,
    val categories: List<CategoryBudgetStatus>
)

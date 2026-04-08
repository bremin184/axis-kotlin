package com.axis.app.ui

import com.axis.app.data.model.Transaction
import com.axis.app.domain.budget.CategoryBudgetStatus

enum class BalanceMode {
    PERSONAL,
    BUSINESS
}

data class HealthInput(
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val totalDebt: Double,
    val budgetStatuses: List<CategoryBudgetStatus>,
    val monthlyExpenseHistory: List<Double>,
    val transactions: List<Transaction>
)

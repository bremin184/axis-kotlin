package com.axis.app.domain.scoring

import com.axis.app.data.model.Transaction
import com.axis.app.domain.budget.BudgetAlertLevel
import com.axis.app.domain.budget.CategoryBudgetStatus

object FinancialHealthCalculator {

    fun calculate(input: HealthInput): HealthScore {
        val incomeScore = (input.monthlyIncome / 1000).toInt().coerceIn(0, 20)
        
        // Fix: Ensure spending score is not negative if expenses > income
        val spendingRatio = if (input.monthlyIncome > 0) (input.monthlyExpenses / input.monthlyIncome) else 1.0
        val spendingScore = ((1 - spendingRatio) * 20).toInt().coerceIn(0, 20)
        
        val debtScore = if (input.monthlyIncome > 0) {
            (20 - (input.totalDebt / input.monthlyIncome * 10)).toInt().coerceIn(0, 20)
        } else 0
        val budgetScore = (20 - input.budgetStatuses.count { it.alertLevel == BudgetAlertLevel.EXCEEDED } * 5).coerceIn(0, 20)
        val savingsScore = (input.transactions.count { it.type == "SAVINGS" } * 2).coerceIn(0, 20)

        val total = (incomeScore + spendingScore + debtScore + budgetScore + savingsScore).coerceIn(0, 100)

        val rating = when {
            total >= 80 -> HealthRating.EXCELLENT
            total >= 60 -> HealthRating.GOOD
            total >= 40 -> HealthRating.FAIR
            else -> HealthRating.POOR
        }

        return HealthScore(
            total = total,
            incomeScore = incomeScore,
            spendingScore = spendingScore,
            debtScore = debtScore,
            budgetScore = budgetScore,
            savingsScore = savingsScore,
            rating = rating
        )
    }

    fun calculateSavingsPercent(
        totalIncome: Double,
        totalExpenses: Double
    ): Double {
        if (totalIncome <= 0.0) return 0.0
        val savings = totalIncome - totalExpenses
        if (savings <= 0.0) return 0.0
        val percent = (savings / totalIncome) * 100.0
        return percent.coerceIn(0.0, 100.0)
    }
}

data class HealthInput(
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val totalDebt: Double,
    val budgetStatuses: List<CategoryBudgetStatus>,
    val monthlyExpenseHistory: List<Double>,
    val transactions: List<Transaction>
)

data class HealthScore(
    val total: Int,
    val incomeScore: Int,
    val spendingScore: Int,
    val debtScore: Int,
    val budgetScore: Int,
    val savingsScore: Int,
    val rating: HealthRating
)

enum class HealthRating(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor")
}

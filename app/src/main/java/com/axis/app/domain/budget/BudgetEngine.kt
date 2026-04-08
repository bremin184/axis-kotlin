package com.axis.app.domain.budget

import com.axis.app.data.model.Budget
import com.axis.app.data.model.CategoryTotal
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.domain.scoring.FinancialHealthCalculator

class BudgetEngine {

    /**
     * Calculates the spending status for each budget category.
     */
    fun calculateStatuses(
        budgets: List<Budget>,
        categoryTotals: List<CategoryTotal>
    ): BudgetOverview {
        if (budgets.isEmpty()) {
            return BudgetOverview(0.0, 0.0, 0f, emptyList())
        }

        val totalsMap = categoryTotals.associate { it.category to it.total }
        var totalBudget = 0.0
        var totalSpent = 0.0

        val statuses = budgets.map { budget ->
            val spent = totalsMap[budget.category] ?: 0.0
            totalBudget += budget.limitAmount
            totalSpent += spent

            val percentage = if (budget.limitAmount > 0) {
                ((spent / budget.limitAmount) * 100).toFloat().coerceAtLeast(0f)
            } else {
                0f
            }

            CategoryBudgetStatus(
                category = budget.category,
                spent = spent,
                limit = budget.limitAmount,
                percentage = percentage,
                alertLevel = getAlertLevel(percentage)
            )
        }

        val overallPercentage = if (totalBudget > 0) {
            ((totalSpent / totalBudget) * 100).toFloat().coerceAtLeast(0f)
        } else {
            0f
        }

        return BudgetOverview(totalBudget, totalSpent, overallPercentage, statuses)
    }

    private fun getAlertLevel(percentage: Float): BudgetAlertLevel {
        return when {
            percentage >= 100 -> BudgetAlertLevel.EXCEEDED
            percentage >= 85 -> BudgetAlertLevel.WARNING
            percentage >= 60 -> BudgetAlertLevel.WATCH
            else -> BudgetAlertLevel.ON_TRACK
        }
    }

    fun generateSuggestions(overview: BudgetOverview, income: Double): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        // 1. Savings Rate Suggestion
        if (income > 0) {
            val savingsRate = FinancialHealthCalculator.calculateSavingsPercent(income, overview.totalSpent)
            if (savingsRate < 15) {
                suggestions.add(
                    Suggestion(
                        SuggestionType.SAVINGS_RATE,
                        "Your savings rate is %.1f%%. Aim for at least 15-20%% by reducing spending or increasing income."
                            .format(savingsRate)
                    )
                )
            }
        }

        // 2. Overspending Warnings
        overview.categories.filter { it.alertLevel == BudgetAlertLevel.EXCEEDED }.forEach { status ->
            val categoryName = CategoryClassifier.CATEGORY_META[status.category]?.label ?: status.category
            suggestions.add(
                Suggestion(
                    SuggestionType.WARNING,
                    "You\'ve exceeded your budget for ${categoryName}. Review recent transactions to cut back."
                )
            )
        }

        // 3. Reallocation Suggestions (find a high-spending, non-essential category)
        val reallocatable = overview.categories
            .filter { it.percentage > 70 && !isEssential(it.category) }
            .maxByOrNull { it.spent }

        if (reallocatable != null) {
            val categoryName = CategoryClassifier.CATEGORY_META[reallocatable.category]?.label ?: reallocatable.category
            suggestions.add(
                Suggestion(
                    SuggestionType.REALLOCATION,
                    "Spending on ${categoryName} is high. Consider reallocating some of this to savings or debt repayment."
                )
            )
        }

        return suggestions
    }

    private fun isEssential(category: String): Boolean {
        return category in listOf("rent", "utilities", "groceries", "transport", "debt")
    }
}

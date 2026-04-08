package com.axis.app.domain.scoring

data class ImprovementTip(
    val title: String,
    val message: String,
    val priority: TipPriority
)

enum class TipPriority { HIGH, MEDIUM, LOW }

object AnalyticsHelper {

    fun generateTips(score: HealthScore): List<ImprovementTip> {
        val tips = mutableListOf<ImprovementTip>()

        if (score.incomeScore < 10) {
            tips.add(ImprovementTip(
                "Increase Income",
                "Your monthly income is below targeted levels for your age group. Consider side gigs or skill upgrades.",
                TipPriority.HIGH
            ))
        }

        if (score.spendingScore < 12) {
            tips.add(ImprovementTip(
                "Reduce Expenses",
                "You are spending a large portion of your income. Aim to keep non-essential spending below 30%.",
                TipPriority.HIGH
            ))
        }

        if (score.debtScore < 15) {
            tips.add(ImprovementTip(
                "Manage Debt",
                "Debt interest might be eating into your wealth. Focus on paying off high-interest loans first.",
                TipPriority.MEDIUM
            ))
        }

        if (score.budgetScore < 15) {
            tips.add(ImprovementTip(
                "Stick to Budgets",
                "You frequently exceed your category limits. Use the monthly budget card to track spending daily.",
                TipPriority.MEDIUM
            ))
        }

        if (score.savingsScore < 10) {
            tips.add(ImprovementTip(
                "Automate Savings",
                "You aren't moving enough funds to savings. Setup an automated M-Shwari or Lock Savings goal.",
                TipPriority.HIGH
            ))
        }

        return tips.sortedBy { it.priority }
    }
}

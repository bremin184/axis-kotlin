package com.axis.app.domain.goals

import com.axis.app.data.model.SavingsGoal
import com.axis.app.data.model.Transaction
import java.util.concurrent.TimeUnit

class GoalProjectionEngine {

    fun calculateProjection(goal: SavingsGoal, transactions: List<Transaction>): GoalProjection {
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(goal.deadline - System.currentTimeMillis()).toInt().coerceAtLeast(0)
        val required = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val dailyAvgRequired = if (daysRemaining > 0) required / daysRemaining else required

        val recentSavings = transactions
            .filter { it.isIncome && it.type == "SAVINGS" }
            .take(30)
            .sumOf { it.amount }
        val monthlyAvgSavings = if (recentSavings > 0) recentSavings else 0.0

        val willMeetGoal = if (monthlyAvgSavings > 0) {
            (goal.currentAmount + (monthlyAvgSavings * (daysRemaining / 30.0))) >= goal.targetAmount
        } else false

        return GoalProjection(
            daysRemaining = daysRemaining,
            required = required,
            dailyAverageRequired = dailyAvgRequired,
            willMeetGoal = willMeetGoal
        )
    }
}

data class GoalProjection(
    val daysRemaining: Int,
    val required: Double,
    val dailyAverageRequired: Double,
    val willMeetGoal: Boolean
)

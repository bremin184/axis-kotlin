package com.axis.app.domain.patterns

sealed class Pattern {
    data class RecurringPayment(
        val recipient: String,
        val frequency: Int,
        val averageAmount: Double,
        val lastTransactionDate: Long
    ) : Pattern()

    data class AnomalousSpending(
        val recipient: String,
        val amount: Double,
        val date: Long
    ) : Pattern()
}

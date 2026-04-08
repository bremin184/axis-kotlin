package com.axis.app.domain.patterns

import com.axis.app.data.model.MerchantFrequency
import com.axis.app.data.model.Transaction

class PatternDetector {

    /**
     * Identifies frequent transactions to the same recipient, which might indicate
     * a subscription or a regular payment that could be budgeted for.
     */
    fun detectFrequentMerchants(
        transactions: List<Transaction>,
        merchantFrequencies: List<MerchantFrequency>
    ): List<Pattern> {
        val frequentMerchants = merchantFrequencies.filter { it.frequency >= 3 && it.totalAmount / it.frequency > 100 }
        val recurringPayments = frequentMerchants.mapNotNull { freq ->
            val merchantTransactions = transactions.filter { it.recipient == freq.recipient }
            val averageAmount = merchantTransactions.map { it.amount }.average()
            val lastTransaction = merchantTransactions.maxByOrNull { it.transactionDateTime }!!

            Pattern.RecurringPayment(
                recipient = freq.recipient,
                frequency = freq.frequency,
                averageAmount = averageAmount,
                lastTransactionDate = lastTransaction.transactionDateTime
            )
        }
        return recurringPayments
    }

    /**
     * Identifies sudden large, one-off expenses that deviate from the user's
     * normal spending pattern.
     */
    fun detectAnomalousSpending(transactions: List<Transaction>): List<Pattern> {
        if (transactions.size < 10) return emptyList()

        val amounts = transactions.filter { !it.isIncome }.map { it.amount }
        val averageExpense = amounts.average()
        val stdDev = calculateStdDev(amounts)

        val anomalies = transactions.filter { txn ->
            !txn.isIncome && txn.amount > averageExpense + (2.5 * stdDev)
        }

        return anomalies.map {
            Pattern.AnomalousSpending(
                recipient = it.recipient ?: "Unknown",
                amount = it.amount,
                date = it.transactionDateTime
            )
        }
    }

    private fun calculateStdDev(data: List<Double>): Double {
        val mean = data.average()
        val variance = data.sumOf { (it - mean) * (it - mean) } / data.size
        return kotlin.math.sqrt(variance)
    }
}

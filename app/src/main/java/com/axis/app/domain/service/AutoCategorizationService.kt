package com.axis.app.domain.service

import com.axis.app.data.database.CategoryRuleDao
import com.axis.app.data.model.Transaction

class AutoCategorizationService(
    private val categoryRuleDao: CategoryRuleDao
) {

    /**
     * Categorizes a transaction based on rules, heuristics, and income status.
     */
    suspend fun categorize(transaction: Transaction): String {
        val message = transaction.rawMessage.uppercase()

        // RULE 1 — INCOME DETECTION
        // All incoming money is categorized as "Income" (Step 11)
        if (transaction.isIncome) {
            return "Income"
        }

        // RULE 2 — CHECK SAVED RULES (Learning)
        val rule = categoryRuleDao.findMatchingRule(message)
        if (rule != null) {
            return rule.category
        }

        // RULE 3 — DEFAULT HEURISTICS (Step 5)
        return defaultCategorize(message)
    }

    private fun defaultCategorize(message: String): String {
        return when {
            message.contains("SAFARICOM") && message.contains("BUNDLE") -> "Data & Internet"
            message.contains("AIRTIME") -> "Airtime"
            message.contains("KPLC") -> "Utilities"
            message.contains("NAIVAS") || message.contains("QUICKMART") -> "Groceries"
            message.contains("UBER") || message.contains("BOLT") -> "Transport"
            else -> "uncategorized"
        }
    }
}

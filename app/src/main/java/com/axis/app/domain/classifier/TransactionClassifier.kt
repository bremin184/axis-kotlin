package com.axis.app.domain.classifier

import com.axis.app.data.model.FundType
import com.axis.app.data.model.SubType

data class FundClassification(
    val fundType: FundType,
    val subType: SubType
)

object TransactionClassifier {

    fun classify(text: String, activeServices: List<String>): FundClassification {
        val lower = text.lowercase()

        // High-priority keywords for savings/investments
        when {
            lower.contains("zidii") -> return FundClassification(FundType.INVESTMENT, SubType.INVESTMENT)
            lower.contains("m-shwari") -> {
                return when {
                    lower.contains("lock savings") -> FundClassification(FundType.SAVINGS, SubType.SHORT_TERM_SAVINGS)
                    else -> FundClassification(FundType.SAVINGS, SubType.SHORT_TERM_SAVINGS)
                }
            }
            activeServices.any { lower.contains(it.lowercase()) } ->
                return FundClassification(FundType.SAVINGS, SubType.SHORT_TERM_SAVINGS)
        }

        // Standard M-Pesa transaction types
        when {
            lower.contains("paid to") -> return FundClassification(FundType.PERSONAL, SubType.PAY_BILL)
            lower.contains("sent to") -> return FundClassification(FundType.PERSONAL, SubType.SEND_MONEY)
            lower.contains("goods and services") || lower.contains("till") -> return FundClassification(FundType.BUSINESS, SubType.BUY_GOODS)
            lower.contains("pochi la biashara") || lower.contains("poch") -> return FundClassification(FundType.BUSINESS, SubType.POCHI_BUSINESS)
            lower.contains("you have received") -> return FundClassification(FundType.PERSONAL, SubType.DEPOSIT)
            lower.contains("withdraw") -> return FundClassification(FundType.PERSONAL, SubType.WITHDRAW)
            lower.contains("buy airtime") -> return FundClassification(FundType.PERSONAL, SubType.AIRTIME)
            lower.contains("reversal") -> return FundClassification(FundType.PERSONAL, SubType.REVERSAL)
            lower.contains("fuliza") -> return FundClassification(FundType.SHORT_TERM_LIABILITY, SubType.LOAN)
            lower.contains("nhif") -> return FundClassification(FundType.HEALTH_INSURANCE, SubType.HEALTH_INSURANCE)
        }

        // Default if no specific pattern is matched
        return FundClassification(FundType.UNKNOWN, SubType.UNKNOWN)
    }
}

package com.axis.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core transaction entity stored in Room.
 * Every parsed M-Pesa SMS becomes one Transaction row.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["accountId", "transactionDateTime"],
            name = "index_transactions_account_datetime"
        ),
        Index(
            value = ["transactionCode"],
            unique = true,
            name = "index_transactions_transactionCode"
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Link to the account this transaction belongs to */
    val accountId: Long = 0,

    /** M-Pesa confirmation code, e.g. "QJK9AB12CD" */
    val transactionCode: String,

    /** Transaction amount in KES */
    val amount: Double,

    /** M-Pesa balance after this transaction */
    val balanceAfter: Double,

    /** Type: SENT, RECEIVED, PAYBILL, BUY_GOODS, WITHDRAW, AIRTIME, DEPOSIT, REVERSAL, LOAN, INVESTMENT */
    val type: String,

    /** Fund classification from the decision tree */
    val fundType: FundType,

    /** Granular sub-type, e.g. POCHI_BUSINESS, MSHWARI_LOAN */
    val subType: String,

    /** Spending category: food_drink, transport, groceries, utilities, etc. */
    val category: String = "uncategorized",

    /** Recipient or merchant name */
    val recipient: String? = null,

    /** Phone number of recipient (if person-to-person) */
    val recipientPhone: String? = null,

    /** Zidii investment ticker symbol, if applicable */
    val ticker: String? = null,

    /** Transaction cost charged by M-Pesa */
    val transactionCost: Double = 0.0,

    /** Actual transaction datetime from SMS (epoch millis) */
    val transactionDateTime: Long,

    /** The original raw SMS text */
    val rawMessage: String,

    /** Whether this is an income (received) or expense (sent) */
    val isIncome: Boolean = false
)

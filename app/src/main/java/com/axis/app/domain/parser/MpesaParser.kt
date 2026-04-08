package com.axis.app.domain.parser

import com.axis.app.data.model.FundType
import com.axis.app.data.model.SubType
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedTransaction(
    val transactionCode: String,
    val amount: Double,
    val balanceAfter: Double,
    val type: String,
    val fundType: FundType,
    val subType: SubType,
    val recipient: String?,
    val recipientPhone: String?,
    val ticker: String?,
    val transactionCost: Double,
    val parsedTimestamp: Long,
    val rawMessage: String,
    val isIncome: Boolean
)

data class ParseResult(
    val transactions: List<ParsedTransaction>,
    val errors: List<String>,
    val total: Int
)

object MpesaParser {
    
    // Support multiple date formats - moved to object level for efficiency
    private val sdf12 = SimpleDateFormat("d/M/yy h:mm a", Locale.US)
    private val sdf24 = SimpleDateFormat("d/M/yyyy HH:mm", Locale.US)
    private val sdfShort24 = SimpleDateFormat("d/M/yy HH:mm", Locale.US)

    // Pre-compiled Regexes for performance
    private val REGEX_CODE = Regex("""^([A-Z0-9]{10})\s+Confirmed\.""")
    private val REGEX_AMOUNT = Regex("""Ksh\s*([0-9,]+\.[0-9]{2})""")
    private val REGEX_BALANCE = Regex("""balance is Ksh\s*([0-9,]+\.[0-9]{2})""")
    private val REGEX_DATE_TIME = Regex("""on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}(?:\s+[AP]M)?)""", RegexOption.IGNORE_CASE)
    private val REGEX_COST = Regex("""cost,\s*Ksh\s*([0-9,]+\.[0-9]{2})""")
    
    private val REGEX_RECIPIENT_PAID = Regex("""paid to (.*?)\.?\s+on""")
    private val REGEX_RECIPIENT_SENT = Regex("""sent to (.*?)\s+on""")
    private val REGEX_RECIPIENT_RECEIVED = Regex("""from (.*?)\s+on""")
    private val REGEX_RECIPIENT_WITHDRAW = Regex("""from (.*?)\.?\s+on""")

    fun parseList(messages: List<String>): ParseResult {
        val transactions = mutableListOf<ParsedTransaction>()
        val errors = mutableListOf<String>()
        
        messages.forEach { message ->
            parse(message)?.let {
                transactions.add(it)
            } ?: errors.add(message)
        }
        return ParseResult(transactions, errors, messages.size)
    }

    fun parse(message: String): ParsedTransaction? {
        return try {
            // 1. Transaction Code
            val codeMatch = REGEX_CODE.find(message)
            val transactionCode = codeMatch?.groupValues?.get(1) ?: throw Exception("Code not found")
            
            // 2. Amount
            val amountMatch = REGEX_AMOUNT.find(message)
            val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDouble() ?: 0.0
            
            // 3. Balance After
            val balanceMatch = REGEX_BALANCE.find(message)
            val balanceAfter = balanceMatch?.groupValues?.get(1)?.replace(",", "")?.toDouble() ?: 0.0
            
            // 4. Date and Time Extraction
            val dateMatch = REGEX_DATE_TIME.find(message)
            val datePart = dateMatch?.groupValues?.get(1) ?: ""
            val timePart = dateMatch?.groupValues?.get(2) ?: ""
            val dateStr = "$datePart $timePart"
            
            var timestamp = System.currentTimeMillis()
            if (dateStr.isNotBlank()) {
                timestamp = try {
                    when {
                        timePart.uppercase().contains(Regex("[AP]M")) -> sdf12.parse(dateStr)?.time ?: timestamp
                        datePart.length > 8 -> sdf24.parse(dateStr)?.time ?: timestamp
                        else -> sdfShort24.parse(dateStr)?.time ?: timestamp
                    }
                } catch (e: Exception) {
                    timestamp
                }
            }
            
            // 5. Transaction Cost
            val costMatch = REGEX_COST.find(message)
            val transactionCost = costMatch?.groupValues?.get(1)?.replace(",", "")?.toDouble() ?: 0.0
            
            // 6. Type, Recipient and Direction
            var type = "UNKNOWN"
            var recipient: String? = null
            var isIncome = false
            
            when {
                message.contains("paid to") -> {
                    type = "PAY_BILL"
                    recipient = REGEX_RECIPIENT_PAID.find(message)?.groupValues?.get(1)
                }
                message.contains("sent to") -> {
                    type = "SEND_MONEY"
                    recipient = REGEX_RECIPIENT_SENT.find(message)?.groupValues?.get(1)
                }
                message.contains("received Ksh") -> {
                    type = "DEPOSIT"
                    isIncome = true
                    recipient = REGEX_RECIPIENT_RECEIVED.find(message)?.groupValues?.get(1)
                }
                message.contains("bought of airtime") -> {
                    type = "AIRTIME"
                    recipient = "Safaricom Airtime"
                }
                message.contains("Withdraw") -> {
                    type = "WITHDRAW"
                    recipient = REGEX_RECIPIENT_WITHDRAW.find(message)?.groupValues?.get(1)
                }
                message.contains("deposited") -> {
                    type = "DEPOSIT"
                    isIncome = true
                }
            }

            ParsedTransaction(
                transactionCode = transactionCode,
                amount = amount,
                balanceAfter = balanceAfter,
                type = type,
                fundType = FundType.UNKNOWN,
                subType = SubType.UNKNOWN,
                recipient = recipient?.trim(),
                recipientPhone = null,
                ticker = null,
                transactionCost = transactionCost,
                parsedTimestamp = timestamp,
                rawMessage = message,
                isIncome = isIncome
            )
        } catch (e: Exception) {
            null
        }
    }
}

package com.axis.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.axis.app.AxisApplication
import com.axis.app.domain.parser.MpesaParser
import com.axis.app.domain.classifier.TransactionClassifier
import com.axis.app.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real-time SMS receiver for M-Pesa transactions.
 */
class MpesaSmsReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: ""
            val body = sms.messageBody ?: ""

            if (sender.contains("MPESA", ignoreCase = true)) {
                Log.d("MpesaSmsReceiver", "M-Pesa SMS detected: $body")
                val pendingResult = goAsync()
                processSms(context, body, pendingResult)
            }
        }
    }

    private fun processSms(context: Context, body: String, pendingResult: PendingResult) {
        val application = context.applicationContext as? AxisApplication ?: return
        val repository = application.repository

        scope.launch {
            try {
                val parsed = MpesaParser.parse(body) ?: return@launch
                val classification = TransactionClassifier.classify(body, emptyList())
                
                // Note: repository.insertTransactions handles auto-categorization via AutoCategorizationService
                val transaction = Transaction(
                    transactionCode = parsed.transactionCode,
                    amount = parsed.amount,
                    balanceAfter = parsed.balanceAfter,
                    type = parsed.type,
                    fundType = classification.fundType,
                    subType = classification.subType.name,
                    category = "uncategorized", // Will be filled by repository
                    recipient = parsed.recipient,
                    recipientPhone = parsed.recipientPhone,
                    ticker = parsed.ticker,
                    transactionCost = parsed.transactionCost,
                    transactionDateTime = parsed.parsedTimestamp,
                    rawMessage = parsed.rawMessage,
                    isIncome = parsed.isIncome
                )

                repository.insertTransactions(listOf(transaction))
                Log.d("MpesaSmsReceiver", "Successfully processed and saved transaction: ${parsed.transactionCode}")
                
                // Show instant intelligence nudge
                com.axis.app.ui.util.NotificationHelper.showTransactionNudge(
                    context, 
                    transaction.amount, 
                    transaction.recipient, 
                    transaction.category
                )
            } catch (e: Exception) {
                Log.e("MpesaSmsReceiver", "Error processing SMS: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

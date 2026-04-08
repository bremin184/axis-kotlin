package com.axis.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.axis.app.AxisApplication
import com.axis.app.data.model.Transaction
import com.axis.app.datastore.UserSettings
import com.axis.app.domain.classifier.TransactionClassifier
import com.axis.app.domain.parser.MpesaParser
import com.axis.app.sms.SmsReader
import kotlinx.coroutines.flow.first

class TransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as AxisApplication
        val repository = application.repository
        val userSettings = UserSettings(applicationContext)
        val smsReader = SmsReader(application.contentResolver)

        val lastTimestamp = userSettings.lastProcessedTimestamp.first()
        val messages = smsReader.readMpesaSms(lastTimestamp)

        if (messages.isEmpty()) {
            return Result.success()
        }

        val rawMessages = messages.map { it.body }
        val parseResult = MpesaParser.parseList(rawMessages)

        val classified = parseResult.transactions.map { parsed ->
            val classification = TransactionClassifier.classify(parsed.rawMessage, emptyList())

            Transaction(
                transactionCode = parsed.transactionCode,
                amount = parsed.amount,
                balanceAfter = parsed.balanceAfter,
                type = parsed.type,
                fundType = classification.fundType,
                subType = classification.subType.name,
                category = "uncategorized",
                recipient = parsed.recipient,
                recipientPhone = parsed.recipientPhone,
                ticker = parsed.ticker,
                transactionCost = parsed.transactionCost,
                transactionDateTime = parsed.parsedTimestamp,
                rawMessage = parsed.rawMessage,
                isIncome = parsed.isIncome
            )
        }

        repository.insertTransactions(classified)

        // Update lastProcessedTimestamp to newest SMS so next sync is incremental
        val newestTimestamp = messages.maxOf { it.timestamp }
        userSettings.setLastProcessedTimestamp(newestTimestamp)

        return Result.success()
    }
}

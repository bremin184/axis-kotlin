package com.axis.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.axis.app.data.database.AppDatabase
import com.axis.app.data.model.FundType
import com.axis.app.ui.util.NotificationHelper
import java.util.*

class SavingsReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Only trigger on the 4th
        if (dayOfMonth != 4) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val transactionDao = db.transactionDao()

        // Calculate range from 1st to 3rd of current month
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startRange = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 3)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endRange = calendar.timeInMillis

        // Check for any SAVINGS or INVESTMENT transactions in this range
        val savingsCount = transactionDao.getCountByFundTypeInDateRange(
            listOf(FundType.SAVINGS.name, FundType.INVESTMENT.name),
            startRange,
            endRange
        )

        // Check if savings accounts exist
        val accountsExist = db.savingsAccountDao().getAllSavingsAccountsList().isNotEmpty()

        if (accountsExist && savingsCount == 0) {
            NotificationHelper.showSavingsReminder(applicationContext)
        }

        return Result.success()
    }
}

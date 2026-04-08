package com.axis.app

import android.app.Application
import com.axis.app.data.database.AppDatabase
import com.axis.app.data.repository.PesaRepository
import com.axis.app.data.repository.TransactionRepository
import com.axis.app.datastore.UserSettings
import com.axis.app.work.SyncManager
import kotlinx.coroutines.launch

class AxisApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userSettings: UserSettings by lazy { UserSettings(this) }

    val repository: PesaRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            savingsGoalDao = database.savingsGoalDao(),
            activatedServiceDao = database.activatedServiceDao(),
            savingsAccountDao = database.savingsAccountDao(),
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            categorizationRuleDao = database.categorizationRuleDao(),
            categoryRuleDao = database.categoryRuleDao(),
            db = database
        )
    }

    override fun onCreate() {
        super.onCreate()
        SyncManager.scheduleSync(this)
        
        // Initialize default categories
        kotlinx.coroutines.MainScope().launch {
            repository.createDefaultCategories()
        }
    }
}

package com.axis.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axis.app.data.model.*
import com.axis.app.data.repository.PesaRepository
import com.axis.app.datastore.UserSettings
import com.axis.app.domain.budget.BudgetEngine
import com.axis.app.domain.budget.BudgetOverview
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.domain.classifier.TransactionClassifier
import com.axis.app.domain.goals.GoalProjectionEngine
import com.axis.app.domain.parser.MpesaParser
import com.axis.app.domain.patterns.PatternDetector
import com.axis.app.domain.scoring.*
import com.axis.app.domain.utils.*
import com.axis.app.sms.SmsReader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.security.MessageDigest

class MainViewModel(
    private val repository: PesaRepository,
    val budgetEngine: BudgetEngine,
    private val application: Application,
    private val userSettings: UserSettings
) : ViewModel() {

    // Engines
    // Engines are now singletons: MpesaParser, TransactionClassifier, CategoryClassifier
    private val goalProjectionEngine = GoalProjectionEngine()
    private val patternDetector = PatternDetector()

    // ========== UI State ==========

    val isDarkMode = userSettings.isDarkMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val userName = userSettings.userName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val userEmail = userSettings.userEmail.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val isSecurityEnabled = userSettings.isSecurityEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isBiometricEnabled = userSettings.isBiometricEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val encPin = userSettings.encPin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val currency = userSettings.currency.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Ksh"
    )

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _balanceMode = MutableStateFlow(BalanceMode.PERSONAL)
    val balanceMode: StateFlow<BalanceMode> = _balanceMode.asStateFlow()

    private val _budgetOverview = MutableStateFlow<BudgetOverview?>(null)
    val budgetOverview: StateFlow<BudgetOverview?> = _budgetOverview.asStateFlow()

    private val _goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val goals: StateFlow<List<SavingsGoal>> = _goals.asStateFlow()

    private val _savingsAccounts = MutableStateFlow<List<SavingsAccount>>(emptyList())
    val savingsAccounts: StateFlow<List<SavingsAccount>> = _savingsAccounts.asStateFlow()

    val allAccountEntities: StateFlow<List<AccountEntity>> = repository.allAccountEntities.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _healthScoreDetail = MutableStateFlow<HealthScore?>(null)
    val healthScoreDetail: StateFlow<HealthScore?> = _healthScoreDetail.asStateFlow()

    private val _budgetProgress = MutableStateFlow(0f)
    val budgetProgress: StateFlow<Float> = _budgetProgress.asStateFlow()

    private val _healthScoreFactor = MutableStateFlow(0f)
    val healthScoreFactor: StateFlow<Float> = _healthScoreFactor.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _monthlyExpenses = MutableStateFlow(0.0)
    val monthlyExpenses: StateFlow<Double> = _monthlyExpenses.asStateFlow()

    private val _monthlyTrend = MutableStateFlow(0.0) // % change from prev month
    val monthlyTrend: StateFlow<Double> = _monthlyTrend.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportState>(ImportState.Idle)
    val importProgress: StateFlow<ImportState> = _importProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    private val _filteredCategoryTotals = MutableStateFlow<List<CategoryTotal>>(emptyList())
    val filteredCategoryTotals: StateFlow<List<CategoryTotal>> = _filteredCategoryTotals.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()
    
    // Check if security should be active on launch
    init {
        viewModelScope.launch {
            val enabled = userSettings.isSecurityEnabled.first()
            if (enabled) {
                _isAppLocked.value = true
            }
        }
        viewModelScope.launch {
            repository.allTransactions.collect { txs ->
                _transactions.value = txs
                updateBalance()
                applyFilters()
            }
        }

        viewModelScope.launch {
            combine(_searchQuery, _selectedCategory) { query, category ->
                Pair(query, category)
            }.collect {
                applyFilters()
            }
        }

        viewModelScope.launch {
            repository.allGoals.collect { goalList ->
                _goals.value = goalList
            }
        }

        viewModelScope.launch {
            repository.allSavingsAccounts.collect { accounts ->
                _savingsAccounts.value = accounts
            }
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value
        val categoryFilter = _selectedCategory.value
        val allTxs = _transactions.value

        val filtered = allTxs.filter { tx ->
            val matchesQuery = if (query.isBlank()) true else {
                tx.recipient?.contains(query, ignoreCase = true) == true ||
                tx.category.contains(query, ignoreCase = true) ||
                tx.type.contains(query, ignoreCase = true) ||
                tx.subType.contains(query, ignoreCase = true)
            }
            val matchesCategory = if (categoryFilter == null) true else {
                tx.category == categoryFilter
            }
            matchesQuery && matchesCategory
        }

        _filteredTransactions.value = filtered
        updateFilteredTotals(filtered)
    }

    private fun updateFilteredTotals(filtered: List<Transaction>) {
        viewModelScope.launch {
            val totals = if (filtered.isEmpty()) {
                emptyList()
            } else {
                repository.getCategoryTotalsForIds(filtered.map { it.id })
            }
            _filteredCategoryTotals.value = totals
            
            // Also update budget overview if we are not filtering
            if (_searchQuery.value.isBlank() && _selectedCategory.value == null) {
                val budgets = repository.getActiveBudgetsList()
                _budgetOverview.value = budgetEngine.calculateStatuses(budgets, totals)
            }
        }
    }

    fun setBalanceMode(mode: BalanceMode) {
        _balanceMode.value = mode
        updateBalance()
    }

    private fun updateBalance() {
        viewModelScope.launch {
            _balance.value = when (_balanceMode.value) {
                BalanceMode.PERSONAL -> repository.getLatestBalance()
                BalanceMode.BUSINESS -> repository.getBusinessBalance()
            }
        }
    }

    fun importFromSmsInbox(startDate: Long) {
        viewModelScope.launch {
            _importProgress.value = ImportState.Loading("Reading SMS inbox...")

            try {
                val smsReader = SmsReader(application.contentResolver)
                val messages = smsReader.readMpesaSms(startDate)

                _importProgress.value = ImportState.Loading("Parsing ${messages.size} messages...")

                val rawTexts = messages.map { it.body }
                processAndImport(rawTexts)

            } catch (e: Exception) {
                _importProgress.value = ImportState.Error("Failed to read SMS: ${e.message}")
            }
        }
    }

    fun importFromText(text: String) {
        viewModelScope.launch {
            _importProgress.value = ImportState.Loading("Parsing messages...")
            val messages = text.split(Regex("""\n\s*\n|\n(?=[A-Z0-9]{10}\s)"""))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            processAndImport(messages)
        }
    }

    private suspend fun processAndImport(rawMessages: List<String>) {
        val activeKeywords = repository.getActiveKeywords()
        val parseResult = MpesaParser.parseList(rawMessages)

        _importProgress.value = ImportState.Loading("Classifying ${parseResult.transactions.size} transactions...")

        val classified = parseResult.transactions.map { parsed ->
            val classification = TransactionClassifier.classify(parsed.rawMessage, activeKeywords.keys.toList())
            val category = CategoryClassifier.classify(
                type = parsed.type,
                recipient = parsed.recipient,
                rawMessage = parsed.rawMessage,
                fundSubType = classification.subType.name
            )

            Transaction(
                transactionCode = parsed.transactionCode,
                amount = parsed.amount,
                balanceAfter = parsed.balanceAfter,
                type = parsed.type,
                fundType = classification.fundType,
                subType = classification.subType.name,
                category = category,
                recipient = parsed.recipient,
                recipientPhone = parsed.recipientPhone,
                ticker = parsed.ticker,
                transactionCost = parsed.transactionCost,
                transactionDateTime = parsed.parsedTimestamp,
                rawMessage = parsed.rawMessage,
                isIncome = parsed.isIncome
            )
        }

        val (added, duplicates) = repository.insertTransactions(classified)

        // Save the newest transaction datetime so background sync starts from near here
        val newestTimestamp = classified.maxOfOrNull { it.transactionDateTime } ?: 0L
        if (newestTimestamp > 0L) {
            userSettings.setLastProcessedTimestamp(newestTimestamp)
        }

        _importProgress.value = ImportState.Success(
            added = added,
            duplicates = duplicates,
            errors = parseResult.errors.size,
            total = parseResult.total,
            isManual = true
        )

        refreshAll()
    }


    fun calculateGoalProgress(goal: SavingsGoal): Double {
        if (goal.savingsAccountId == null || goal.allocationPercentage == null) {
            return goal.currentAmount
        }
        val account = _savingsAccounts.value.find { it.id == goal.savingsAccountId }
        return if (account != null) {
            account.balance * goal.allocationPercentage
        } else {
            goal.currentAmount
        }
    }

    fun upsertGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.upsertGoal(goal)
        }
    }

    private suspend fun refreshAll() {
        _isLoading.value = true

        val txs = repository.getAllTransactionsList()
        _balance.value = repository.getLatestBalance()

        val cal = Calendar.getInstance()
        val endCurrent = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startCurrent = cal.timeInMillis

        // Previous month
        cal.add(Calendar.MONTH, -1)
        val startPrev = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endPrev = cal.timeInMillis

        _monthlyIncome.value = repository.getMonthlyIncome(startCurrent, endCurrent)
        _monthlyExpenses.value = repository.getMonthlyExpenses(startCurrent, endCurrent)
        val prevExpenses = repository.getMonthlyExpenses(startPrev, endPrev)
        
        _monthlyTrend.value = if (prevExpenses > 0) {
            ((_monthlyExpenses.value - prevExpenses) / prevExpenses * 100)
        } else 0.0

        val budgets = repository.getActiveBudgetsList()
        val categoryTotals = repository.getMonthlyCategoryTotals(startCurrent, endCurrent)
        _budgetOverview.value = budgetEngine.calculateStatuses(budgets, categoryTotals)
        
        applyFilters()

        val debtPayments = repository.getMonthlyDebtPayments(startCurrent, endCurrent)
        val hScore = FinancialHealthCalculator.calculate(
            com.axis.app.domain.scoring.HealthInput(
                monthlyIncome = _monthlyIncome.value.coerceAtLeast(1.0),
                monthlyExpenses = _monthlyExpenses.value,
                totalDebt = debtPayments ?: 0.0,
                budgetStatuses = _budgetOverview.value?.categories ?: emptyList(),
                monthlyExpenseHistory = listOf(prevExpenses, _monthlyExpenses.value),
                transactions = txs
            )
        )
        _healthScoreDetail.value = hScore
        _healthScoreFactor.value = hScore.total / 100f

        // Calculate budget progress factor
        val overview = _budgetOverview.value
        _budgetProgress.value = if (overview != null && overview.totalBudget > 0) {
            (overview.totalSpent / overview.totalBudget).toFloat().coerceIn(0f, 1f)
        } else 0f

        _isLoading.value = false
    }

    fun updateBudgetLimit(category: String, newLimit: Double) {
        viewModelScope.launch {
            repository.upsertBudget(Budget(category, newLimit))
            val budgets = repository.getActiveBudgetsList()
            val categoryTotals = repository.getMonthlyCategoryTotals()
            _budgetOverview.value = budgetEngine.calculateStatuses(budgets, categoryTotals)
        }
    }

    fun addGoal(name: String, target: Double, deadline: Long) {
        viewModelScope.launch {
            val existingGoals = repository.getAllGoalsList()
            repository.upsertGoal(
                SavingsGoal(
                    name = name,
                    targetAmount = target,
                    deadline = deadline,
                    isPrimary = existingGoals.isEmpty()
                )
            )
        }
    }

    fun addToGoal(goalId: Long, addAmount: Double) {
        viewModelScope.launch {
            val goals = repository.getAllGoalsList()
            val goal = goals.find { it.id == goalId } ?: return@launch
            repository.updateGoalAmount(goalId, goal.currentAmount + addAmount)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.initDefaultBudgets()
            userSettings.setLastProcessedTimestamp(0L)
            
            // Explicitly clear UI state to prevent stale data
            _transactions.value = emptyList()
            _balance.value = 0.0
            _monthlyIncome.value = 0.0
            _monthlyExpenses.value = 0.0
            _monthlyTrend.value = 0.0
            _budgetOverview.value = null
            _healthScoreDetail.value = null
            _budgetProgress.value = 0f
            _healthScoreFactor.value = 0f
            
            refreshAll()
        }
    }

    fun resetImportState() {
        _importProgress.value = ImportState.Idle
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            userSettings.setDarkMode(isDark)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userSettings.setUserName(name)
        }
    }

    fun updateUserEmail(email: String) {
        viewModelScope.launch {
            userSettings.setUserEmail(email)
        }
    }

    fun updateSharedPin(pin: String) {
        viewModelScope.launch {
            val hashed = hashPin(pin)
            userSettings.setEncPin(hashed)
            userSettings.setSecurityEnabled(true)
        }
    }

    private fun hashPin(pin: String): String {
        return try {
            val bytes = pin.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            pin // Fallback to plain for unexpected errors, though SHA-256 is standard
        }
    }

    fun unlockApp(pin: String): Boolean {
        // Logic will be handled in MainActivity for Biometric, this is for PIN fallback
        viewModelScope.launch {
            val stored = userSettings.encPin.first()
            if (hashPin(pin) == stored) {
                _isAppLocked.value = false
            }
        }
        return false // Return value is not used as we use the StateFlow
    }
    
    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun setSecurityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setSecurityEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setBiometricEnabled(enabled)
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            userSettings.setCurrency(currency)
        }
    }

    fun exportFinancialSummary(context: android.content.Context) {
        viewModelScope.launch {
            val mode = balanceMode.value.name
            
            val summary = FinancialSummary(
                totalIncome = monthlyIncome.value,
                totalExpenses = monthlyExpenses.value,
                savingsRate = FinancialHealthCalculator.calculateSavingsPercent(monthlyIncome.value, monthlyExpenses.value),
                categoryBreakdown = repository.getMonthlyCategoryTotals().associate { it.category to it.total },
                mode = mode
            )
            val file = ExportUtils.exportToCsv(context, summary)
            if (file != null) {
                ExportUtils.shareFile(context, file)
            }
        }
    }

    fun exportProfessionalAnalysis(context: android.content.Context) {
        viewModelScope.launch {
            val name = if (userName.value.isBlank()) "Axis User" else userName.value
            val income = monthlyIncome.value
            val expenses = monthlyExpenses.value
            val expenseRatio = if (income > 0) (expenses / income).coerceIn(0.0, 1.0) else 1.0
            val health = healthScoreDetail.value
            val trend = monthlyTrend.value
            val efficiency = (health?.savingsScore ?: 0) * 5
            
            val categories = repository.getMonthlyCategoryTotals().map { it.category to it.total }
            
            val analysis = ProfessionalAnalysis(
                userName = name,
                monthlyIncomeTrend = trend,
                expenseRatio = expenseRatio,
                savingsEfficiency = efficiency,
                topCategories = categories,
                healthScore = health?.total ?: 0,
                riskIndicators = getRiskIndicators(health),
                recommendations = getRecommendations(health)
            )
            
            val file = ExportUtils.exportToPdf(context, analysis)
            if (file != null) {
                ExportUtils.shareFile(context, file)
            }
        }
    }

    private fun getRiskIndicators(score: HealthScore?): List<String> {
        if (score == null) return emptyList()
        val risks = mutableListOf<String>()
        if (score.spendingScore < 10) risks.add("High spending relative to income (>80%)")
        if (score.debtScore < 10) risks.add("High debt-to-income ratio")
        if (score.budgetScore < 10) risks.add("Frequent budget overruns")
        if (score.savingsScore < 5) risks.add("Critically low savings activity")
        return risks
    }

    private fun getRecommendations(score: HealthScore?): List<String> {
        if (score == null) return listOf("Start tracking transactions to get insights.")
        val recs = mutableListOf<String>()
        if (score.spendingScore < 15) recs.add("Reduce non-essential entertainment and shopping.")
        if (score.savingsScore < 15) recs.add("Set up an automatic savings goal for at least 10% of income.")
        if (score.budgetScore < 15) recs.add("Review and increase your transport or grocery budgets.")
        if (recs.isEmpty()) recs.add("Your financial habits are strong. Consider investing in long-term goals.")
        return recs
    }

    fun clearImportedData() {
        viewModelScope.launch {
            repository.clearImportedData()
            userSettings.setLastProcessedTimestamp(0L)
            
            // Explicitly clear UI state
            _transactions.value = emptyList()
            _balance.value = 0.0
            _monthlyIncome.value = 0.0
            _monthlyExpenses.value = 0.0
            _monthlyTrend.value = 0.0
            _budgetOverview.value = null
            
            refreshAll()
        }
    }

    fun importDemoData() {
        viewModelScope.launch {
            if (_transactions.value.isNotEmpty()) {
                _importProgress.value = ImportState.Error("Demo data can only be loaded when the database is empty.")
                return@launch
            }

            _importProgress.value = ImportState.Loading("Loading demo data...")

            val sampleMessages = listOf(
                "QJK9AB12CD Confirmed. Ksh250.00 paid to JAVA HOUSE NAIROBI. on 22/2/26 at 8:30 AM. New M-PESA balance is Ksh45,320.00. Transaction cost, Ksh0.00.",
                "QJK9AB13EF Confirmed. Ksh3,500.00 paid to SHELL MOMBASA RD. on 22/2/26 at 10:15 AM. New M-PESA balance is Ksh41,820.00. Transaction cost, Ksh0.00.",
                "QJK9AB14GH Confirmed. Ksh100.00 paid to SAFARICOM AIRTIME. on 22/2/26 at 11:00 AM. New M-PESA balance is Ksh41,720.00. Transaction cost, Ksh0.00.",
                "QJK9AB15IJ Confirmed. Ksh1,200.00 sent to JOHN KAMAU 0712345678 on 22/2/26 at 2:30 PM. New M-PESA balance is Ksh40,520.00. Transaction cost, Ksh22.00.",
                "QJK9AB16KL Confirmed. You have received Ksh25,000.00 from ACME CORP 0720111222 on 22/2/26 at 5:00 PM. New M-PESA balance is Ksh65,520.00.",
                "QJK8CD17MN Confirmed. Ksh4,500.00 paid to NAIVAS SUPERMARKET. on 21/2/26 at 9:00 AM. New M-PESA balance is Ksh20,320.00. Transaction cost, Ksh0.00.",
                "QJK8CD18OP Confirmed. Ksh2,200.00 paid to UBER BV. on 21/2/26 at 1:45 PM. New M-PESA balance is Ksh18,120.00. Transaction cost, Ksh0.00.",
                "QJK8CD19QR Confirmed. Ksh800.00 paid to NETFLIX. on 21/2/26 at 7:00 PM. New M-PESA balance is Ksh17,320.00. Transaction cost, Ksh0.00.",
                "QJK7EF20ST Confirmed. Ksh12,000.00 paid to LANDLORD APARTMENTS. on 20/2/26 at 10:00 AM. New M-PESA balance is Ksh5,320.00. Transaction cost, Ksh35.00.",
                "QJK7EF21UV Confirmed. You have received Ksh50,000.00 from EMPLOYER LTD 0733444555 on 20/2/26 at 9:00 AM. New M-PESA balance is Ksh55,320.00.",
                "QJK6GH22WX Confirmed. Ksh3,200.00 paid to KPLC PREPAID. on 19/2/26 at 11:30 AM. New M-PESA balance is Ksh52,120.00. Transaction cost, Ksh0.00.",
                "QJK6GH23YZ Confirmed. Ksh1,500.00 paid to CLEANSHELF SUPERMARKET. on 19/2/26 at 4:00 PM. New M-PESA balance is Ksh50,620.00. Transaction cost, Ksh0.00.",
                "QJK5IJ24AB Confirmed. Ksh500.00 sent to MAMA NJERI 0723456789 on 18/2/26 at 12:00 PM. New M-PESA balance is Ksh50,120.00. Transaction cost, Ksh15.00.",
                "QJK5IJ25CD Confirmed. Ksh350.00 paid to ARTCAFFE JUNCTION. on 18/2/26 at 6:30 PM. New M-PESA balance is Ksh49,770.00. Transaction cost, Ksh0.00.",
                "QJK4KL26EF Confirmed. Ksh2,000.00 paid to JUMIA ONLINE. on 17/2/26 at 3:15 PM. New M-PESA balance is Ksh47,770.00. Transaction cost, Ksh0.00.",
                "QJK4KL27GH Confirmed. Ksh150.00 bought of airtime on 17/2/26 at 8:00 AM. New M-PESA balance is Ksh47,620.00. Transaction cost, Ksh0.00.",
                "QJK3MN28IJ Confirmed. Ksh5,000.00 paid to NAIROBI WATER. on 16/2/26 at 10:00 AM. New M-PESA balance is Ksh42,620.00. Transaction cost, Ksh0.00.",
                "QJK3MN29KL Confirmed. You have received Ksh8,000.00 from FREELANCE CLIENT 0711222333 on 15/2/26 at 2:00 PM. New M-PESA balance is Ksh50,620.00.",
                "QJK2OP30MN Confirmed. Ksh6,000.00 paid to CARREFOUR WESTGATE. on 14/2/26 at 5:30 PM. New M-PESA balance is Ksh44,620.00. Transaction cost, Ksh0.00.",
                "QJK2OP31QR Confirmed. Ksh1,800.00 paid to BOLT RIDES. on 13/2/26 at 7:45 AM. New M-PESA balance is Ksh42,820.00. Transaction cost, Ksh0.00."
            )

            processAndImport(sampleMessages)
        }
    }

    // ========================
    // Category Management
    // ========================

    fun categorizeTransaction(transactionId: Long, categoryName: String, recipient: String?, rawMessage: String? = null) {
        viewModelScope.launch {
            repository.categorizeTransaction(transactionId, categoryName, recipient, rawMessage)
            refreshAll()
        }
    }

    fun addCustomCategory(name: String, type: CategoryType = CategoryType.EXPENSE_ONLY) {
        viewModelScope.launch {
            repository.addCategory(
                Category(name = name, isDefault = false, type = type)
            )
        }
    }

    fun updateCategoryName(categoryId: Long, newName: String) {
        viewModelScope.launch {
            repository.updateCategoryName(categoryId, newName)
        }
    }

    fun deleteCustomCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.upsertAccount(account)
        }
    }

    fun addCustomAccount(name: String, type: AccountType) {
        viewModelScope.launch {
            repository.upsertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    isEnabled = true,
                    balance = 0.0,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

sealed class ImportState {
    data object Idle : ImportState()
    data class Loading(val message: String) : ImportState()
    data class Success(val added: Int, val duplicates: Int, val errors: Int, val total: Int, val isManual: Boolean = false) : ImportState()
    data class Error(val message: String) : ImportState()
}

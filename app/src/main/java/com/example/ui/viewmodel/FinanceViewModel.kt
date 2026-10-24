package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.FinancialBadge
import com.example.data.model.FinancialNote
import com.example.data.model.Goal
import com.example.data.model.ImportHistory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.domain.analytics.BudgetUtilisation
import com.example.domain.analytics.CategorySpending
import com.example.domain.analytics.DailySpending
import com.example.domain.analytics.DetectedRecurringPayment
import com.example.domain.analytics.FinancialAnalytics
import com.example.domain.analytics.FinancialInsight
import com.example.domain.analytics.MonthProjection
import com.example.domain.analytics.RecurringPaymentAnalyzer
import com.example.domain.analytics.SavingsRecommendation
import com.example.domain.analytics.TimePeriod
import com.example.domain.parser.ParsedTransaction
import com.example.domain.parser.StatementParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class DashboardUiState(
    val totalExpensesToday: Double = 0.0,
    val totalExpensesThisWeek: Double = 0.0,
    val totalExpensesThisMonth: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpenses: Double = 0.0,
    val periodNetCashFlow: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBudget: Double = 2500.0,
    val budgetRemaining: Double = 2500.0,
    val budgetProgress: Float = 0f,
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val dailyTrend: List<DailySpending> = emptyList(),
    val projection: MonthProjection? = null,
    val streakDays: Int = 0,
    val recentTransactions: List<Transaction> = emptyList(),
    val upcomingBills: List<Bill> = emptyList(),
    val activeGoals: List<Goal> = emptyList()
)

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    // Repository Flows
    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<FinancialNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allImportHistory: StateFlow<List<ImportHistory>> = repository.allImportHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Period Filter
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    // Transactions Search / Filters / Sorting
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        sortOrder
    ) { txs, query, typeFilter, catFilter, sort ->
        var list = txs

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.description.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.account.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.referenceId.lowercase().contains(q) ||
                it.amount.toString().contains(q)
            }
        }

        if (typeFilter != null) {
            list = list.filter { it.type == typeFilter }
        }

        if (!catFilter.isNullOrBlank()) {
            list = list.filter { it.category.equals(catFilter, ignoreCase = true) }
        }

        when (sort) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.timestamp }
            SortOrder.DATE_ASC -> list.sortedBy { it.timestamp }
            SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Combined State (Accurate without double-counting)
    val dashboardState: StateFlow<DashboardUiState> = combine(
        allTransactions,
        userSettings,
        selectedPeriod,
        allBills,
        allGoals
    ) { txs, settings, period, bills, goals ->
        val periodTxs = FinancialAnalytics.filterTransactionsByPeriod(txs, period)
        val periodIncome = periodTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val periodGrossExpense = periodTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val periodRefunds = periodTxs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
        val periodNetExpense = (periodGrossExpense - periodRefunds).coerceAtLeast(0.0)
        val periodNet = periodIncome - periodNetExpense

        val totalExpensesToday = FinancialAnalytics.calculateExpensesToday(txs)
        val totalExpensesThisWeek = FinancialAnalytics.calculateExpensesThisWeek(txs)
        val totalExpensesThisMonth = FinancialAnalytics.calculateExpensesThisMonth(txs)

        val allIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val allExpenses = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val allRefunds = txs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
        val netBalance = settings.startingBalance + allIncome + allRefunds - allExpenses

        val budgetLimit = settings.monthlyBudget
        val budgetRemaining = (budgetLimit - totalExpensesThisMonth).coerceAtLeast(0.0)
        val budgetProgress = if (budgetLimit > 0) (totalExpensesThisMonth / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

        val categoryBreakdown = FinancialAnalytics.calculateCategoryBreakdown(periodTxs)
        val dailyTrend = FinancialAnalytics.calculateDailySpendingTrend(txs, 7)
        val projection = FinancialAnalytics.calculateMonthProjection(budgetLimit, totalExpensesThisMonth)
        val streak = FinancialAnalytics.calculateStreak(txs)

        DashboardUiState(
            totalExpensesToday = totalExpensesToday,
            totalExpensesThisWeek = totalExpensesThisWeek,
            totalExpensesThisMonth = totalExpensesThisMonth,
            periodIncome = periodIncome,
            periodExpenses = periodNetExpense,
            periodNetCashFlow = periodNet,
            netBalance = netBalance,
            monthlyBudget = budgetLimit,
            budgetRemaining = budgetRemaining,
            budgetProgress = budgetProgress,
            categoryBreakdown = categoryBreakdown,
            dailyTrend = dailyTrend,
            projection = projection,
            streakDays = streak,
            recentTransactions = txs.take(5),
            upcomingBills = bills.filter { !it.isPaidThisMonth }.take(4),
            activeGoals = goals.filter { !it.isCompleted }.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // Recurring Payments Detection
    val recurringPayments: StateFlow<List<DetectedRecurringPayment>> = allTransactions.combine(allBudgets) { txs, _ ->
        RecurringPaymentAnalyzer.detectRecurringPayments(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Utilisation per category
    val budgetUtilisation: StateFlow<List<BudgetUtilisation>> = combine(allBudgets, allTransactions) { budgets, txs ->
        val monthTxs = FinancialAnalytics.filterTransactionsByPeriod(txs, TimePeriod.MONTH)
        RecurringPaymentAnalyzer.computeBudgetUtilisation(budgets, monthTxs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savings Recommendations
    val savingsRecommendations: StateFlow<List<SavingsRecommendation>> = combine(allTransactions, userSettings) { txs, settings ->
        RecurringPaymentAnalyzer.generateSavingsRecommendations(txs, settings.monthlyBudget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Badges Flow
    val badges: StateFlow<List<FinancialBadge>> = combine(
        allTransactions,
        allGoals,
        userSettings
    ) { txs, goals, settings ->
        val streak = FinancialAnalytics.calculateStreak(txs)
        FinancialAnalytics.calculateBadges(txs, goals, streak, settings.monthlyBudget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Insights Flow
    val insights: StateFlow<List<FinancialInsight>> = combine(
        allTransactions,
        allGoals,
        userSettings
    ) { txs, goals, settings ->
        FinancialAnalytics.generateInsights(txs, settings.monthlyBudget, goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statement Import Preview States
    private val _importedPreviewList = MutableStateFlow<List<ParsedTransaction>>(emptyList())
    val importedPreviewList: StateFlow<List<ParsedTransaction>> = _importedPreviewList.asStateFlow()

    private val _importFileName = MutableStateFlow("")
    val importFileName: StateFlow<String> = _importFileName.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    fun parseStatementContent(content: String, fileName: String) {
        viewModelScope.launch {
            _isParsing.value = true
            _importFileName.value = fileName
            val parsed = StatementParser.parseStatement(
                content = content,
                fileName = fileName,
                existingTransactions = allTransactions.value,
                defaultCurrency = userSettings.value.currency
            )
            _importedPreviewList.value = parsed
            _isParsing.value = false
            val dups = parsed.count { it.isDuplicate }
            _messageEvent.emit("Parsed ${parsed.size} records ($dups duplicates identified)")
        }
    }

    fun toggleParsedTransactionSelection(tempId: String) {
        _importedPreviewList.value = _importedPreviewList.value.map {
            if (it.tempId == tempId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllParsed(select: Boolean) {
        _importedPreviewList.value = _importedPreviewList.value.map {
            it.copy(isSelected = select)
        }
    }

    fun excludeDuplicatesFromImport() {
        _importedPreviewList.value = _importedPreviewList.value.map {
            if (it.isDuplicate) it.copy(isSelected = false) else it
        }
    }

    fun updateParsedCategory(tempId: String, newCategory: String) {
        _importedPreviewList.value = _importedPreviewList.value.map {
            if (it.tempId == tempId) it.copy(category = newCategory) else it
        }
    }

    fun updateParsedType(tempId: String, newType: TransactionType) {
        _importedPreviewList.value = _importedPreviewList.value.map {
            if (it.tempId == tempId) it.copy(type = newType) else it
        }
    }

    fun confirmImport(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val selected = _importedPreviewList.value.filter { it.isSelected }
            if (selected.isEmpty()) {
                _messageEvent.emit("No transactions selected for import")
                return@launch
            }

            val totalParsed = _importedPreviewList.value.size
            val totalDuplicates = _importedPreviewList.value.count { it.isDuplicate }
            val fileName = _importFileName.value.ifBlank { "Statement Import" }
            val source = if (fileName.contains("phonepe", true)) "PhonePe Statement" else if (fileName.contains("pdf", true)) "PDF Statement" else "Bank Statement"

            repository.importStatementTransactions(
                fileName = fileName,
                source = source,
                currency = userSettings.value.currency,
                totalParsed = totalParsed,
                totalDuplicates = totalDuplicates,
                selectedTransactions = selected
            )

            _messageEvent.emit("Successfully imported ${selected.size} transactions!")
            _importedPreviewList.value = emptyList()
            _importFileName.value = ""
            onComplete(selected.size)
        }
    }

    fun clearImportPreview() {
        _importedPreviewList.value = emptyList()
        _importFileName.value = ""
    }

    fun deleteImportBatch(batchId: Long) {
        viewModelScope.launch {
            repository.deleteImportBatch(batchId)
            _messageEvent.emit("Imported statement batch deleted")
        }
    }

    // Snackbars / Toast events
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    // Last deleted transaction for Undo
    private var lastDeletedTransaction: Transaction? = null

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            _messageEvent.emit("Transaction saved")
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _messageEvent.emit("Transaction updated")
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            lastDeletedTransaction = transaction
            repository.deleteTransaction(transaction)
            _messageEvent.emit("Transaction deleted")
        }
    }

    fun undoDeleteTransaction() {
        val tx = lastDeletedTransaction ?: return
        viewModelScope.launch {
            repository.addTransaction(tx)
            lastDeletedTransaction = null
            _messageEvent.emit("Transaction restored")
        }
    }

    // Budgets
    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            repository.setBudget(budget)
            _messageEvent.emit("Budget category updated")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _messageEvent.emit("Budget deleted")
        }
    }

    // Goals
    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            repository.addGoal(goal)
            _messageEvent.emit("Savings goal created")
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
            _messageEvent.emit("Goal updated")
        }
    }

    fun contributeToGoal(goal: Goal, amount: Double) {
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            _messageEvent.emit("Added $amount to ${goal.title}")
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _messageEvent.emit("Goal removed")
        }
    }

    // Bills
    fun addBill(bill: Bill) {
        viewModelScope.launch {
            repository.addBill(bill)
            _messageEvent.emit("Bill reminder added")
        }
    }

    fun toggleBillPaid(bill: Bill) {
        viewModelScope.launch {
            repository.toggleBillPaid(bill)
            val status = if (!bill.isPaidThisMonth) "marked as paid" else "marked as unpaid"
            _messageEvent.emit("${bill.title} $status")
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            _messageEvent.emit("Bill removed")
        }
    }

    // Notes
    fun addNote(note: FinancialNote) {
        viewModelScope.launch {
            repository.addNote(note)
            _messageEvent.emit("Note added to workspace")
        }
    }

    fun updateNote(note: FinancialNote) {
        viewModelScope.launch {
            repository.updateNote(note)
            _messageEvent.emit("Note updated")
        }
    }

    fun deleteNote(note: FinancialNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _messageEvent.emit("Note deleted")
        }
    }

    // Settings
    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            repository.updateCurrency(currency)
            _messageEvent.emit("Currency updated to $currency")
        }
    }

    fun updateMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(budget)
            _messageEvent.emit("Monthly budget updated")
        }
    }

    fun updateStartingBalance(balance: Double) {
        viewModelScope.launch {
            repository.updateStartingBalance(balance)
            _messageEvent.emit("Starting balance updated")
        }
    }

    fun completeOnboarding(startingBalance: Double, monthlyIncome: Double, monthlyBudget: Double) {
        viewModelScope.launch {
            repository.completeOnboarding(startingBalance, monthlyIncome, monthlyBudget)
            _messageEvent.emit("Welcome to My Finance!")
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            val curr = userSettings.value.currency
            repository.seedSampleData(curr)
            _messageEvent.emit("Sample financial data generated!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _messageEvent.emit("All data cleared")
        }
    }

    fun exportToCsv(): String {
        return repository.exportTransactionsToCsv(allTransactions.value)
    }

    fun importFromCsv(csvContent: String, onComplete: (Pair<Int, Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importTransactionsFromCsv(csvContent, userSettings.value.currency)
            _messageEvent.emit("Imported ${result.first} transactions")
            onComplete(result)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val prefsRepo = UserPreferencesRepository(application)
                    val repository = FinanceRepository(
                        transactionDao = db.transactionDao(),
                        budgetDao = db.budgetDao(),
                        goalDao = db.goalDao(),
                        billDao = db.billDao(),
                        financialNoteDao = db.financialNoteDao(),
                        importHistoryDao = db.importHistoryDao(),
                        userPreferencesRepository = prefsRepo
                    )
                    return FinanceViewModel(application, repository) as T
                }
            }
    }
}

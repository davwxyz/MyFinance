package com.example.data.repository

import com.example.data.local.BillDao
import com.example.data.local.BudgetDao
import com.example.data.local.FinancialNoteDao
import com.example.data.local.GoalDao
import com.example.data.local.ImportHistoryDao
import com.example.data.local.TransactionDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.FinancialNote
import com.example.data.model.Goal
import com.example.data.model.ImportHistory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.parser.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val billDao: BillDao,
    private val financialNoteDao: FinancialNoteDao,
    private val importHistoryDao: ImportHistoryDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allBills: Flow<List<Bill>> = billDao.getAllBills()
    val allNotes: Flow<List<FinancialNote>> = financialNoteDao.getAllNotes()
    val allImportHistory: Flow<List<ImportHistory>> = importHistoryDao.getAllImportHistory()
    val userSettings: Flow<UserSettings> = userPreferencesRepository.userSettingsFlow

    suspend fun addTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    // Budgets
    suspend fun setBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    // Goals
    suspend fun addGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }

    suspend fun contributeToGoal(goal: Goal, amount: Double) {
        val newAmount = (goal.currentAmount + amount).coerceAtLeast(0.0)
        val isCompleted = newAmount >= goal.targetAmount
        goalDao.updateGoal(
            goal.copy(
                currentAmount = newAmount,
                isCompleted = isCompleted
            )
        )
    }

    // Bills
    suspend fun addBill(bill: Bill): Long {
        return billDao.insertBill(bill)
    }

    suspend fun updateBill(bill: Bill) {
        billDao.updateBill(bill)
    }

    suspend fun toggleBillPaid(bill: Bill) {
        val updated = bill.copy(isPaidThisMonth = !bill.isPaidThisMonth)
        billDao.updateBill(updated)
    }

    suspend fun deleteBill(bill: Bill) {
        billDao.deleteBill(bill)
    }

    // Notes
    suspend fun addNote(note: FinancialNote): Long {
        return financialNoteDao.insertNote(note)
    }

    suspend fun updateNote(note: FinancialNote) {
        financialNoteDao.updateNote(note)
    }

    suspend fun deleteNote(note: FinancialNote) {
        financialNoteDao.deleteNote(note)
    }

    // User Preferences
    suspend fun updateCurrency(currency: String) {
        userPreferencesRepository.updateCurrency(currency)
    }

    suspend fun updateMonthlyBudget(budget: Double) {
        userPreferencesRepository.updateMonthlyBudget(budget)
    }

    suspend fun updateStartingBalance(balance: Double) {
        userPreferencesRepository.updateStartingBalance(balance)
    }

    suspend fun updateThemeMode(theme: String) {
        userPreferencesRepository.updateThemeMode(theme)
    }

    suspend fun completeOnboarding(startingBalance: Double, monthlyIncome: Double, monthlyBudget: Double) {
        userPreferencesRepository.completeOnboarding(startingBalance, monthlyIncome, monthlyBudget)
    }

    // Export CSV
    fun exportTransactionsToCsv(transactions: List<Transaction>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Currency,Category,Description,Account,Date,Notes,Tags\n")
        transactions.forEach { tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val safeDesc = tx.description.replace(",", ";").replace("\"", "'")
            val safeNotes = tx.notes.replace(",", ";").replace("\"", "'")
            val safeTags = tx.tags.replace(",", ";").replace("\"", "'")
            sb.append("${tx.id},${tx.type.name},${tx.amount},${tx.currency},${tx.category},\"$safeDesc\",${tx.account},$dateStr,\"$safeNotes\",\"$safeTags\"\n")
        }
        return sb.toString()
    }

    // Import CSV
    suspend fun importTransactionsFromCsv(csvContent: String, defaultCurrency: String): Pair<Int, Int> {
        var importedCount = 0
        var skippedCount = 0
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Pair(0, 0)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val altDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val header = lines.first().lowercase()
        val dataLines = if (header.contains("type") && header.contains("amount")) lines.drop(1) else lines

        val parsedTransactions = mutableListOf<Transaction>()

        for (line in dataLines) {
            try {
                val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                    .map { it.trim().trim('\"') }
                if (tokens.size >= 5) {
                    val typeStr = tokens.getOrNull(1)?.uppercase() ?: "EXPENSE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.EXPENSE }
                    val amount = tokens.getOrNull(2)?.toDoubleOrNull() ?: continue
                    val currency = tokens.getOrNull(3)?.ifBlank { defaultCurrency } ?: defaultCurrency
                    val category = tokens.getOrNull(4)?.ifBlank { "General" } ?: "General"
                    val description = tokens.getOrNull(5) ?: "Imported transaction"
                    val account = tokens.getOrNull(6) ?: "Cash"
                    val dateStr = tokens.getOrNull(7) ?: ""
                    val timestamp = try {
                        dateFormat.parse(dateStr)?.time
                            ?: altDateFormat.parse(dateStr)?.time
                            ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    val notes = tokens.getOrNull(8) ?: ""
                    val tags = tokens.getOrNull(9) ?: ""

                    parsedTransactions.add(
                        Transaction(
                            type = type,
                            amount = amount,
                            currency = currency,
                            category = category,
                            description = description,
                            timestamp = timestamp,
                            account = account,
                            notes = notes,
                            tags = tags
                        )
                    )
                    importedCount++
                } else {
                    skippedCount++
                }
            } catch (_: Exception) {
                skippedCount++
            }
        }

        if (parsedTransactions.isNotEmpty()) {
            transactionDao.insertTransactions(parsedTransactions)
        }

        return Pair(importedCount, skippedCount)
    }

    // Seed realistic sample data
    suspend fun seedSampleData(currency: String = "$") {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000

        val sampleTransactions = listOf(
            Transaction(type = TransactionType.INCOME, amount = 4250.0, currency = currency, category = "Salary", description = "Monthly Tech Lead Salary", timestamp = now - 18 * dayMillis, account = "Bank Account", notes = "Direct deposit"),
            Transaction(type = TransactionType.INCOME, amount = 350.0, currency = currency, category = "Freelance", description = "App Consultation UI/UX", timestamp = now - 5 * dayMillis, account = "Bank Account", notes = "Side project client"),
            Transaction(type = TransactionType.EXPENSE, amount = 1200.0, currency = currency, category = "Housing", description = "Apartment Rent & Service Fee", timestamp = now - 19 * dayMillis, account = "Bank Account", notes = "Monthly lease payment"),
            Transaction(type = TransactionType.EXPENSE, amount = 142.50, currency = currency, category = "Groceries", description = "Whole Foods Organic Market", timestamp = now - 1 * dayMillis, account = "Credit Card", notes = "Weekly essentials and fresh produce"),
            Transaction(type = TransactionType.EXPENSE, amount = 68.20, currency = currency, category = "Food & Dining", description = "Tokyo Ramen House Dinner", timestamp = now - 2 * dayMillis, account = "Debit Card", notes = "Dinner with friends"),
            Transaction(type = TransactionType.EXPENSE, amount = 18.50, currency = currency, category = "Food & Dining", description = "Artisan Espresso & Croissant", timestamp = now, account = "Cash", notes = "Morning cafe run"),
            Transaction(type = TransactionType.EXPENSE, amount = 85.00, currency = currency, category = "Bills & Utilities", description = "Fiber High-Speed Internet", timestamp = now - 10 * dayMillis, account = "Credit Card", notes = "Monthly broadband"),
            Transaction(type = TransactionType.EXPENSE, amount = 45.00, currency = currency, category = "Transport", description = "Metro & Light Rail Monthly Pass", timestamp = now - 14 * dayMillis, account = "Debit Card", notes = "Transit card reload"),
            Transaction(type = TransactionType.EXPENSE, amount = 115.00, currency = currency, category = "Health", description = "Pharmacy Vitamins & Checkup", timestamp = now - 7 * dayMillis, account = "Credit Card", notes = "Supplements and checkup co-pay"),
            Transaction(type = TransactionType.EXPENSE, amount = 39.99, currency = currency, category = "Entertainment", description = "Cinema & Popcorn Night", timestamp = now - 4 * dayMillis, account = "Debit Card", notes = "Weekend film"),
            Transaction(type = TransactionType.EXPENSE, amount = 78.40, currency = currency, category = "Groceries", description = "Trader Joe's Pantry Stock", timestamp = now - 8 * dayMillis, account = "Credit Card", notes = "Snacks and coffee beans"),
            Transaction(type = TransactionType.EXPENSE, amount = 55.00, currency = currency, category = "Personal Care", description = "Haircut & Barber Grooming", timestamp = now - 12 * dayMillis, account = "Cash", notes = "Monthly styling"),
            Transaction(type = TransactionType.TRANSFER, amount = 400.0, currency = currency, category = "Savings", description = "Automatic Emergency Fund Transfer", timestamp = now - 15 * dayMillis, account = "Bank Account", notes = "High yield savings deposit")
        )

        val sampleBudgets = listOf(
            Budget(category = "Food & Dining", monthlyLimit = 350.0),
            Budget(category = "Groceries", monthlyLimit = 450.0),
            Budget(category = "Transport", monthlyLimit = 150.0),
            Budget(category = "Entertainment", monthlyLimit = 120.0),
            Budget(category = "Bills & Utilities", monthlyLimit = 220.0),
            Budget(category = "Health", monthlyLimit = 150.0),
            Budget(category = "Shopping", monthlyLimit = 200.0)
        )

        val sampleGoals = listOf(
            Goal(title = "Emergency Safety Net", targetAmount = 6000.0, currentAmount = 4200.0, iconName = "shield", colorHex = "#2E7D32", notes = "6 months of essential living expenses"),
            Goal(title = "Tokyo Summer Vacation", targetAmount = 2500.0, currentAmount = 1450.0, iconName = "flight", colorHex = "#C62828", notes = "Flights, hotels, and bullet train pass"),
            Goal(title = "New Workstation Setup", targetAmount = 1800.0, currentAmount = 1800.0, iconName = "laptop", colorHex = "#1565C0", isCompleted = true, notes = "M3 Pro MacBook + 4K Display (Target reached!)")
        )

        val sampleBills = listOf(
            Bill(title = "Apartment Rent", amount = 1200.0, category = "Housing", dueDateDay = 1, frequency = "Monthly", isPaidThisMonth = true),
            Bill(title = "Fiber Broadband", amount = 85.0, category = "Bills & Utilities", dueDateDay = 15, frequency = "Monthly", isPaidThisMonth = true),
            Bill(title = "Electric & Energy Bill", amount = 72.0, category = "Bills & Utilities", dueDateDay = 28, frequency = "Monthly", isPaidThisMonth = false),
            Bill(title = "Health Insurance Premium", amount = 160.0, category = "Health", dueDateDay = 25, frequency = "Monthly", isPaidThisMonth = false),
            Bill(title = "Cloud & Streaming Bundle", amount = 29.99, category = "Entertainment", dueDateDay = 22, frequency = "Monthly", isPaidThisMonth = false)
        )

        val sampleNotes = listOf(
            FinancialNote(
                title = "50 / 30 / 20 Budget Strategy",
                content = "• 50% Needs: Rent ($1,200), Groceries ($450), Utilities ($220), Transport ($150) -> ~$2,020\n• 30% Wants: Dining Out ($350), Entertainment ($120), Shopping ($200) -> ~$670\n• 20% Savings: High-Yield Savings ($500) + Roth IRA ($350) -> ~$850\nTotal monthly baseline: $3,540",
                category = "Strategy",
                isPinned = true
            ),
            FinancialNote(
                title = "7 Rules to Cut Impulse Expenses",
                content = "1. Wait 48 hours for any purchase over $50.\n2. Calculate cost in hours worked (e.g. $100 = 4 working hours).\n3. Check if we already own a functional equivalent.\n4. Unsubscribe from promotional retailer newsletters.\n5. Keep separate card for dining & impulse entertainment with hard limit.",
                category = "Checklist",
                isPinned = true
            ),
            FinancialNote(
                title = "End of Year Financial Checklist",
                content = "✓ Max out IRA contribution limit\n✓ Review subscription audit (cancel unused cloud apps)\n✓ Rebalance index fund asset allocation\n✓ File property and healthcare flex receipts",
                category = "Checklist",
                isPinned = false
            )
        )

        transactionDao.insertTransactions(sampleTransactions)
        budgetDao.insertBudgets(sampleBudgets)
        goalDao.insertGoals(sampleGoals)
        billDao.insertBills(sampleBills)
        financialNoteDao.insertNotes(sampleNotes)
    }

    suspend fun importStatementTransactions(
        fileName: String,
        source: String,
        currency: String,
        totalParsed: Int,
        totalDuplicates: Int,
        selectedTransactions: List<ParsedTransaction>
    ): ImportHistory {
        val historyId = importHistoryDao.insertImportHistory(
            ImportHistory(
                fileName = fileName,
                source = source,
                totalParsed = totalParsed,
                totalImported = selectedTransactions.size,
                totalDuplicates = totalDuplicates
            )
        )
        val transactionsToInsert = selectedTransactions.map { it.toTransaction(currency, historyId) }
        transactionDao.insertTransactions(transactionsToInsert)
        return ImportHistory(
            id = historyId,
            fileName = fileName,
            source = source,
            totalParsed = totalParsed,
            totalImported = selectedTransactions.size,
            totalDuplicates = totalDuplicates
        )
    }

    suspend fun deleteImportBatch(batchId: Long) {
        transactionDao.deleteTransactionsByBatchId(batchId)
        importHistoryDao.deleteImportHistoryById(batchId)
    }

    suspend fun clearAllData() {
        transactionDao.clearAll()
        budgetDao.clearAll()
        goalDao.clearAll()
        billDao.clearAll()
        financialNoteDao.clearAll()
        importHistoryDao.clearAll()
    }
}

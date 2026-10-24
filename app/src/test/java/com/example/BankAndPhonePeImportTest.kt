package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ImportHistory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.analytics.FinancialAnalytics
import com.example.domain.analytics.RecurringPaymentAnalyzer
import com.example.domain.analytics.TimePeriod
import com.example.domain.parser.ParsedTransaction
import com.example.domain.parser.StatementParser
import com.example.domain.parser.TransactionCategorizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BankAndPhonePeImportTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPhonePeStatementParsingAndCategorization() {
        val samplePhonePe = """
            Date,Transaction ID,Details,Type,Amount,UTR
            2026-10-24,T261024123456,Paid to Swiggy,DEBIT,340.00,429810234561
            2026-10-23,T261023987654,Refund from Swiggy for Order,CREDIT,120.00,429710987654
            2026-10-22,T261022456789,Paid to Blinkit Grocery,DEBIT,485.50,429610456789
            2026-10-21,T261021789012,Transfer to Self SBI Account,DEBIT,1500.00,429510789012
        """.trimIndent()

        val parsed = StatementParser.parseStatement(
            content = samplePhonePe,
            fileName = "PhonePe_Statement.csv",
            existingTransactions = emptyList()
        )

        assertEquals(4, parsed.size)

        // Swiggy expense
        assertEquals(TransactionType.EXPENSE, parsed[0].type)
        assertEquals("Food & Dining", parsed[0].category)
        assertEquals(340.0, parsed[0].amount, 0.001)
        assertEquals("429810234561", parsed[0].referenceId)

        // Swiggy refund
        assertEquals(TransactionType.REFUND, parsed[1].type)
        assertEquals(120.0, parsed[1].amount, 0.001)

        // Blinkit groceries
        assertEquals(TransactionType.EXPENSE, parsed[2].type)
        assertEquals("Groceries", parsed[2].category)

        // Transfer to self
        assertEquals(TransactionType.TRANSFER, parsed[3].type)
    }

    @Test
    fun testDuplicateDetectionByReferenceId() {
        val existingTx = Transaction(
            id = 1,
            type = TransactionType.EXPENSE,
            amount = 340.0,
            referenceId = "429810234561",
            description = "Swiggy Order"
        )

        val parsedItem = ParsedTransaction(
            timestamp = System.currentTimeMillis(),
            rawDescription = "Paid to Swiggy",
            cleanDescription = "Swiggy",
            amount = 340.0,
            type = TransactionType.EXPENSE,
            category = "Food & Dining",
            account = "PhonePe",
            referenceId = "429810234561",
            source = "PhonePe Statement"
        )

        val evaluated = StatementParser.detectDuplicates(listOf(parsedItem), listOf(existingTx))
        assertEquals(1, evaluated.size)
        assertTrue(evaluated[0].isDuplicate)
        assertFalse(evaluated[0].isSelected) // duplicates default to unselected
        assertTrue(evaluated[0].duplicateReason!!.contains("429810234561"))
    }

    @Test
    fun testDuplicateDetectionByAmountAndCloseTimestamp() {
        val now = System.currentTimeMillis()
        val existingTx = Transaction(
            id = 2,
            type = TransactionType.EXPENSE,
            amount = 450.0,
            timestamp = now,
            description = "Blinkit Groceries"
        )

        // New parsed transaction 2 hours later without ref ID but same amount & description
        val parsedItem = ParsedTransaction(
            timestamp = now + (2 * 60 * 60 * 1000),
            rawDescription = "Blinkit Groceries",
            cleanDescription = "Blinkit Groceries",
            amount = 450.0,
            type = TransactionType.EXPENSE,
            category = "Groceries",
            account = "PhonePe",
            referenceId = "",
            source = "PhonePe Statement"
        )

        val evaluated = StatementParser.detectDuplicates(listOf(parsedItem), listOf(existingTx))
        assertTrue(evaluated[0].isDuplicate)
    }

    @Test
    fun testRefundOffsetsExpenseWithoutDoubleCounting() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            Transaction(type = TransactionType.EXPENSE, amount = 1000.0, category = "Shopping", timestamp = now),
            Transaction(type = TransactionType.REFUND, amount = 250.0, category = "Shopping", timestamp = now),
            Transaction(type = TransactionType.INCOME, amount = 3000.0, category = "Salary", timestamp = now),
            Transaction(type = TransactionType.TRANSFER, amount = 500.0, category = "Transfers", timestamp = now)
        )

        // Total expense should be 1000 - 250 = 750 (not 1000, and refund shouldn't inflate income to 3250)
        val expensesToday = FinancialAnalytics.calculateExpensesToday(txs)
        assertEquals(750.0, expensesToday, 0.001)

        val breakdown = FinancialAnalytics.calculateCategoryBreakdown(txs)
        assertEquals(1, breakdown.size)
        assertEquals(750.0, breakdown[0].amount, 0.001)

        // Transfers are excluded from expense and income
        val monthIncome = FinancialAnalytics.calculateIncomeThisMonth(txs)
        assertEquals(3000.0, monthIncome, 0.001)
    }

    @Test
    fun testRecurringPaymentDetection() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val txs = listOf(
            Transaction(type = TransactionType.EXPENSE, amount = 649.0, description = "Netflix Subscription", timestamp = now - 60 * dayMillis),
            Transaction(type = TransactionType.EXPENSE, amount = 649.0, description = "Netflix Subscription", timestamp = now - 30 * dayMillis),
            Transaction(type = TransactionType.EXPENSE, amount = 649.0, description = "Netflix Subscription", timestamp = now),
            Transaction(type = TransactionType.EXPENSE, amount = 12000.0, description = "Apartment Rent", timestamp = now - 30 * dayMillis),
            Transaction(type = TransactionType.EXPENSE, amount = 12000.0, description = "Apartment Rent", timestamp = now)
        )

        val recurring = RecurringPaymentAnalyzer.detectRecurringPayments(txs)
        assertTrue(recurring.isNotEmpty())
        assertTrue(recurring.any { it.title.contains("Netflix", ignoreCase = true) })
        assertTrue(recurring.any { it.title.contains("Rent", ignoreCase = true) })
    }

    @Test
    fun testRoomDatabasePersistenceAndBatchDeletion() = runBlocking {
        val txDao = db.transactionDao()
        val historyDao = db.importHistoryDao()

        // 1. Insert an import history log
        val historyId = historyDao.insertImportHistory(
            ImportHistory(
                fileName = "PhonePe_Oct.csv",
                source = "PhonePe Statement",
                totalParsed = 10,
                totalImported = 8,
                totalDuplicates = 2
            )
        )
        assertTrue(historyId > 0)

        // 2. Insert transactions tied to this batch
        val tx1 = Transaction(
            type = TransactionType.EXPENSE,
            amount = 250.0,
            description = "Zepto Instant Grocery",
            importBatchId = historyId,
            referenceId = "UTR12345"
        )
        val tx2 = Transaction(
            type = TransactionType.EXPENSE,
            amount = 180.0,
            description = "Uber Transit",
            importBatchId = historyId,
            referenceId = "UTR67890"
        )
        txDao.insertTransactions(listOf(tx1, tx2))

        // Query transactions
        val allTxs = txDao.getAllTransactions().first()
        assertEquals(2, allTxs.size)

        // Query by referenceId
        val match = txDao.getTransactionByReferenceId("UTR12345")
        assertNotNull(match)
        assertEquals("Zepto Instant Grocery", match?.description)

        // Batch rollback / deletion
        txDao.deleteTransactionsByBatchId(historyId)
        historyDao.deleteImportHistoryById(historyId)

        val remainingTxs = txDao.getAllTransactions().first()
        assertEquals(0, remainingTxs.size)

        val remainingHistory = historyDao.getAllImportHistory().first()
        assertEquals(0, remainingHistory.size)
    }
}

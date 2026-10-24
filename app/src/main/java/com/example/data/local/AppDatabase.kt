package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Bill
import com.example.data.model.Budget
import com.example.data.model.FinancialNote
import com.example.data.model.Goal
import com.example.data.model.ImportHistory
import com.example.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        Goal::class,
        Bill::class,
        FinancialNote::class,
        ImportHistory::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun billDao(): BillDao
    abstract fun financialNoteDao(): FinancialNoteDao
    abstract fun importHistoryDao(): ImportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_finance_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

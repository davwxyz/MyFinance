package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double = 0.0,
    val currency: String = "$",
    val category: String = "General",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val account: String = "Cash",
    val notes: String = "",
    val tags: String = "",
    val referenceId: String = "",
    val source: String = "MANUAL", // MANUAL, PHONEPE, BANK_STATEMENT
    val importBatchId: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

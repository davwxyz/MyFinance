package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Bills & Utilities",
    val dueDateDay: Int = 1,
    val frequency: String = "Monthly",
    val isPaidThisMonth: Boolean = false,
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

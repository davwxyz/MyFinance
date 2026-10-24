package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: Int = 0, // 0 for default/all months, or 1-12
    val year: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

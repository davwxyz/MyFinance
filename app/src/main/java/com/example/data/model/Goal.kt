package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000,
    val iconName: String = "savings",
    val colorHex: String = "#C62828",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (targetAmount <= 0.0) 0f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}

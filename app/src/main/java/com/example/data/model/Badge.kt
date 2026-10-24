package com.example.data.model

data class FinancialBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val progress: Float, // 0.0 to 1.0
    val unlockedDate: String = ""
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_notes")
data class FinancialNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Strategy", // Strategy, Checklist, Wishlist, Goal Note
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

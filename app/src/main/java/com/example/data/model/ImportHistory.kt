package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_history")
data class ImportHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val source: String,
    val totalParsed: Int,
    val totalImported: Int,
    val totalDuplicates: Int,
    val importedAt: Long = System.currentTimeMillis()
)
